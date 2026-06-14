package com.example.cyber_budget

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * TransactionsActivity: History of all transactions.
 * Updated: Standardized to use category-specific color dots and reactive sync.
 */
class TransactionsActivity : AppCompatActivity() {

    private val TAG = "Transactions_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvSelectedPeriod: TextView
    private lateinit var cgFilters: ChipGroup
    private lateinit var sharedPreferences: SharedPreferences
    private val listeners = mutableListOf<ListenerRegistration>()
    
    private var categoriesMap = mapOf<String, String>()
    private var categoryColorsMap = mapOf<String, String>()
    private var categoryNameColorsMap = mapOf<String, String>()
    private var expenseDocs = listOf<DocumentSnapshot>()
    private var incomeDocs = listOf<DocumentSnapshot>()

    private var currentFilterType = "All" 
    private var activeCycleRange: BudgetCycleHelper.DateRange? = null
    private var customStartDate: Calendar? = null
    private var customEndDate: Calendar? = null
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            logoutUser()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_transactions)
        HeaderHelper.setupHeader(this)
        
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

        rvTransactions = findViewById(R.id.rv_transactions)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        tvSelectedPeriod = findViewById(R.id.tv_selected_period)
        cgFilters = findViewById(R.id.cg_filters)

        setupFilters()
        setupNavbar()
        
        setPeriodCurrentCycle()
        startRealTimeSync()
    }

    private fun setupFilters() {
        cgFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterType = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> "Income"
                R.id.chip_expense -> "Expense"
                else -> "All"
            }
            combineAndDisplay()
        }

        findViewById<View>(R.id.cv_period_picker).setOnClickListener { showPeriodSelector() }
        findViewById<View>(R.id.btn_filter).setOnClickListener { showPeriodSelector() }
    }

    private fun showPeriodSelector() {
        val options = arrayOf("This Cycle", "Last 30 Days", "Show All Time", "Pick Month/Year", "Custom Range")
        AlertDialog.Builder(this)
            .setTitle("Filter by Period")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setPeriodCurrentCycle()
                    1 -> setPeriodLast30Days()
                    2 -> setPeriodAllTime()
                    3 -> showMonthYearPicker()
                    4 -> showCustomDateRangePicker()
                }
            }.show()
    }

    private fun setPeriodCurrentCycle() {
        val cycleDay = sharedPreferences.getInt("userBudgetCycleDay", 1)
        activeCycleRange = BudgetCycleHelper.getCurrentCycleRange(cycleDay)
        customStartDate = null
        customEndDate = null
        tvSelectedPeriod.text = "This Cycle"
        combineAndDisplay()
    }

    private fun setPeriodLast30Days() {
        activeCycleRange = null
        val end = Calendar.getInstance()
        customEndDate = end
        
        val start = Calendar.getInstance()
        start.add(Calendar.DAY_OF_YEAR, -30)
        customStartDate = start
        
        tvSelectedPeriod.text = "Last 30 Days"
        combineAndDisplay()
    }

    private fun setPeriodAllTime() {
        activeCycleRange = null
        customStartDate = null
        customEndDate = null
        tvSelectedPeriod.text = "All Time"
        combineAndDisplay()
    }

    private fun showCustomDateRangePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val start = Calendar.getInstance()
            start.set(year, month, day, 0, 0, 0)
            customStartDate = start
            
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val end = Calendar.getInstance()
                end.set(y2, m2, d2, 23, 59, 59)
                customEndDate = end
                activeCycleRange = null
                tvSelectedPeriod.text = "${sdf.format(start.time)} to ${sdf.format(end.time)}"
                combineAndDisplay()
            }, year, month, day).show()
            
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showMonthYearPicker() {
        val cal = Calendar.getInstance()
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        val yearPicker = NumberPicker(this).apply { minValue = 2020; maxValue = 2030; value = cal.get(Calendar.YEAR) }
        val monthPicker = NumberPicker(this).apply { minValue = 0; maxValue = 11; displayedValues = months; value = cal.get(Calendar.MONTH) }
        
        val layout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; addView(monthPicker); addView(yearPicker) }

        AlertDialog.Builder(this)
            .setTitle("Select Month & Year")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val start = Calendar.getInstance()
                start.set(yearPicker.value, monthPicker.value, 1, 0, 0, 0)
                customStartDate = start
                
                val end = start.clone() as Calendar
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59)
                customEndDate = end
                
                activeCycleRange = null
                tvSelectedPeriod.text = "${months[monthPicker.value]} ${yearPicker.value}"
                combineAndDisplay()
            }.show()
    }

    private fun logoutUser() {
        auth.signOut()
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        startActivity(Intent(this, login::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }

    private fun startRealTimeSync() {
        val userId = auth.currentUser?.uid ?: return
        
        listeners.add(firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val cycleDay = snapshot.getLong("budgetCycleDay")?.toInt() ?: 1
                    if (cycleDay != sharedPreferences.getInt("userBudgetCycleDay", 1)) {
                        sharedPreferences.edit().putInt("userBudgetCycleDay", cycleDay).apply()
                        if (activeCycleRange != null) setPeriodCurrentCycle()
                    }
                }
            })

        listeners.add(firestore.collection("categories").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                categoriesMap = snapshot?.documents?.associate { it.id to (it.getString("name") ?: "Unknown") } ?: emptyMap()
                categoryColorsMap = snapshot?.documents?.associate { it.id to (it.getString("color") ?: "#808080") } ?: emptyMap()
                categoryNameColorsMap = snapshot?.documents?.associate { (it.getString("name") ?: "Unknown") to (it.getString("color") ?: "#808080") } ?: emptyMap()
                combineAndDisplay()
            })

        listeners.add(firestore.collection("expense_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                expenseDocs = snapshot?.documents ?: emptyList()
                combineAndDisplay()
            })

        listeners.add(firestore.collection("income_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                incomeDocs = snapshot?.documents ?: emptyList()
                combineAndDisplay()
            })
    }

    private fun combineAndDisplay() {
        val uiList = mutableListOf<TransactionUI>()

        expenseDocs.forEach { doc ->
            val dateStr = doc.getString("date") ?: ""
            if (isWithinFilterRange(dateStr)) {
                if (currentFilterType == "All" || currentFilterType == "Expense") {
                    val catId = doc.getString("categoryId") ?: ""
                    uiList.add(TransactionUI(
                        id = doc.id,
                        description = doc.getString("description") ?: "Expense", 
                        categoryName = categoriesMap[catId] ?: "Unknown", 
                        amount = getAmountFromDoc(doc), 
                        date = dateStr, 
                        time = doc.getString("startTime") ?: "", 
                        isIncome = false, 
                        hasPhoto = doc.getString("photoPath") != null, 
                        photoPath = doc.getString("photoPath"),
                        categoryColor = categoryColorsMap[catId] ?: "#808080"
                    ))
                }
            }
        }

        incomeDocs.forEach { doc ->
            val dateStr = doc.getString("date") ?: ""
            if (isWithinFilterRange(dateStr)) {
                if (currentFilterType == "All" || currentFilterType == "Income") {
                    val source = doc.getString("source") ?: "Income"
                    uiList.add(TransactionUI(
                        id = doc.id,
                        description = source, 
                        categoryName = "Income", 
                        amount = getAmountFromDoc(doc), 
                        date = dateStr, 
                        time = "", 
                        isIncome = true,
                        categoryColor = categoryNameColorsMap[source] ?: "#4CAF50"
                    ))
                }
            }
        }

        rvTransactions.adapter = TransactionAdapter(uiList.sortedByDescending { it.date }, { t -> showDeleteConfirmation(t) }, { p -> showFullPhoto(p) })
    }

    private fun isWithinFilterRange(dateStr: String): Boolean {
        if (activeCycleRange != null) return BudgetCycleHelper.isDateInRange(dateStr, activeCycleRange!!)
        if (customStartDate == null || customEndDate == null) return true
        
        return try {
            val date = sdf.parse(dateStr) ?: return true
            val cal = Calendar.getInstance().apply { time = date; set(Calendar.HOUR_OF_DAY, 12) }
            !cal.before(customStartDate) && !cal.after(customEndDate)
        } catch (e: Exception) { true }
    }

    private fun getAmountFromDoc(doc: DocumentSnapshot): Double {
        val amt = doc.get("amount")
        return when (amt) { is Number -> amt.toDouble(); is String -> amt.toDoubleOrNull() ?: 0.0; else -> 0.0 }
    }

    private fun showFullPhoto(path: String) {
        val file = File(path)
        if (!file.exists()) return
        val ivFull = ImageView(this).apply {
            setImageURI(Uri.fromFile(file))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).setView(ivFull).show()
    }

    private fun showDeleteConfirmation(transaction: TransactionUI) {
        AlertDialog.Builder(this).setTitle("Delete Transaction?").setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteTransaction(transaction) }.setNegativeButton("Cancel", null).show()
    }

    private fun deleteTransaction(transaction: TransactionUI) {
        val collection = if (transaction.isIncome) "income_entries" else "expense_entries"
        firestore.collection(collection).document(transaction.id).delete()
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_activity)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java) }
        findViewById<View>(R.id.ll_nav_profile)?.setOnClickListener { navigateTo(SettingsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>) {
        val intent = Intent(this, cls); intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); startActivity(intent)
    }

    override fun onDestroy() { super.onDestroy(); listeners.forEach { it.remove() } }
}
