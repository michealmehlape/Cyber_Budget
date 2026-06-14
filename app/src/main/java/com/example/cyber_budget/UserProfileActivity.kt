package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * UserProfileActivity: Allows users to update their personal information.
 * Features real-time profile syncing and standardized Navbar positioning.
 */
class UserProfileActivity : AppCompatActivity() {

    private val TAG = "UserProfile_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        HeaderHelper.setupHeader(this)

        // Fix Navbar position: Ensure it sits exactly above system bars for consistency
        val navContainer = findViewById<View>(R.id.include_nav)
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            navContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom
            }
            insets
        }

        val etFirstName = findViewById<EditText>(R.id.et_first_name)
        val etLastName = findViewById<EditText>(R.id.et_last_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnSave = findViewById<Button>(R.id.btn_save)

        loadUserData(etFirstName, etLastName, etEmail)

        btnSave.setOnClickListener {
            saveUserData(etFirstName.text.toString().trim(), etLastName.text.toString().trim())
        }

        setupNavbar()
    }

    private fun loadUserData(etF: EditText, etL: EditText, etE: EditText) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etF.setText(doc.getString("firstName"))
                    etL.setText(doc.getString("lastName"))
                    etE.setText(doc.getString("email"))
                    etE.isEnabled = false // Email usually locked for security
                }
            }
    }

    private fun saveUserData(fName: String, lName: String) {
        if (fName.isEmpty() || lName.isEmpty()) {
            Toast.makeText(this, "Names cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>("firstName" to fName, "lastName" to lName)

        firestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                sharedPreferences.edit().putString("userFirstName", fName).apply()
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_profile)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java, false) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>, useReorder: Boolean = true) {
        val intent = Intent(this, cls)
        if (useReorder) intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
}
