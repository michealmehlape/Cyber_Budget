package com.example.cyber_budget

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * AddTransactionActivity: Standardized with the modern UI and real-time sync.
 * Fixed: Camera error solved using FileProvider for high-resolution photo capture.
 */
class AddTransactionActivity : AppCompatActivity() {

    private val TAG = "AddTransaction_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val calendar = Calendar.getInstance()
    private lateinit var ivPreview: ImageView
    private var photoFile: File? = null

    // High-resolution photo capture launcher
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoFile?.let {
                ivPreview.setImageURI(Uri.fromFile(it))
                ivPreview.visibility = View.VISIBLE
            }
        } else {
            photoFile = null
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera()
        else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)
        HeaderHelper.setupHeader(this)

        val navContainer = findViewById<View>(R.id.include_nav)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            navContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom
            }
            insets
        }

        val etAmount = findViewById<EditText>(R.id.et_amount)
        val etDescription = findViewById<EditText>(R.id.et_description)
        val etDate = findViewById<EditText>(R.id.et_date)
        val etStart = findViewById<EditText>(R.id.et_start_time)
        val etEnd = findViewById<EditText>(R.id.et_end_time)
        val spinner = findViewById<Spinner>(R.id.spinner_category)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnAddNewCategory = findViewById<TextView>(R.id.btn_add_category)
        val rgType = findViewById<RadioGroup>(R.id.rg_type)
        val btnTakePhoto = findViewById<Button>(R.id.btn_take_photo)
        ivPreview = findViewById(R.id.iv_preview)

        setupPickers(etDate, etStart, etEnd)

        btnAddNewCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        loadCategories(spinner)
        btnTakePhoto.setOnClickListener { handleCameraAction() }

        btnSave.setOnClickListener {
            saveTransaction(etAmount, etDescription, etDate, etStart, etEnd, spinner, rgType)
        }

        setupNavbar()
    }

    private fun handleCameraAction() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile("receipt_${System.currentTimeMillis()}_", ".jpg", storageDir)
            photoFile = file
            
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating photo file", e)
            Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPickers(etDate: EditText, etStart: EditText, etEnd: EditText) {
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val timeSetListener = { target: EditText ->
            TimePickerDialog(this, { _, h, min ->
                target.setText(String.format(Locale.US, "%02d:%02d", h, min))
            }, 12, 0, true).show()
        }
        etStart.setOnClickListener { timeSetListener(etStart) }
        etEnd.setOnClickListener { timeSetListener(etEnd) }
    }

    private fun loadCategories(spinner: Spinner) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("categories").whereEqualTo("userId", userId)
            .addSnapshotListener { documents, _ ->
                val categoryList = documents?.mapNotNull { it.getString("name") } ?: emptyList()
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
                spinner.adapter = adapter
            }
    }

    private fun saveTransaction(etAmount: EditText, etDesc: EditText, etDate: EditText, etStart: EditText, etEnd: EditText, spinner: Spinner, rgType: RadioGroup) {
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val desc = etDesc.text.toString().trim()
        val date = etDate.text.toString()
        val start = etStart.text.toString()
        val end = etEnd.text.toString()
        val categoryName = spinner.selectedItem?.toString() ?: ""
        
        if (amount <= 0 || date.isEmpty() || categoryName.isEmpty()) {
            Toast.makeText(this, "Amount, Date and Category are required", Toast.LENGTH_SHORT).show()
            return
        }

        val isIncome = rgType.checkedRadioButtonId == R.id.rb_income
        val userId = auth.currentUser?.uid ?: return

        var finalPhotoPath: String? = null
        photoFile?.let { file ->
            val permanentFile = File(filesDir, "receipt_${System.currentTimeMillis()}.jpg")
            file.copyTo(permanentFile, true)
            finalPhotoPath = permanentFile.absolutePath
        }

        if (isIncome) {
            val incomeMap = hashMapOf("userId" to userId, "source" to categoryName, "amount" to amount, "date" to date)
            firestore.collection("income_entries").add(incomeMap).addOnSuccessListener {
                Toast.makeText(this, "Income Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            firestore.collection("categories").whereEqualTo("userId", userId).whereEqualTo("name", categoryName)
                .get().addOnSuccessListener { docs ->
                    val categoryId = docs.documents.firstOrNull()?.id ?: ""
                    val expenseMap = hashMapOf(
                        "userId" to userId, "categoryId" to categoryId, "date" to date,
                        "startTime" to start, "endTime" to end, "description" to desc,
                        "amount" to amount, "photoPath" to finalPhotoPath
                    )
                    firestore.collection("expense_entries").add(expenseMap).addOnSuccessListener {
                        Toast.makeText(this, "Expense Saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
        }
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
        findViewById<View>(R.id.ll_nav_profile)?.setOnClickListener { navigateTo(SettingsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>) {
        val intent = Intent(this, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
}
