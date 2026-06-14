package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity: Primary dashboard.
 * Design Updated: Pie chart now matches the required visual style:
 * - Category names outside with connecting lines.
 * - Percentages displayed inside the slices.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity_Log"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rvRecent: RecyclerView
    private lateinit var pieChart: PieChart
    
    private var categoriesMap = mutableMapOf<String, String>()
    private var categoryColorsMap = mutableMapOf<String, String>()
    private var categoryNameColorsMap = mutableMapOf<String, String>()
    private var allExpenses = listOf<DocumentSnapshot>()
    private var allIncomes = listOf<DocumentSnapshot>()
    private var allBudgetDocs = listOf<DocumentSnapshot>()
    
    private val listeners = mutableListOf<ListenerRegistration>()

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
        setContentView(R.layout.activity_main)
        HeaderHelper.setupHeader(this) 

        val navContainer = findViewById<View>(R.id.include_nav)
        val mainRoot = findViewById<View>(R.id.main)
        
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            navContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom 
            }
            insets
        }

        rvRecent = findViewById(R.id.rv_recent_activity)
        rvRecent.layoutManager = LinearLayoutManager(this)
        pieChart = findViewById(R.id.pieChart)

        setupNavbar()
        startRealTimeSync()
    }

    private fun logoutUser() {
        auth.signOut()
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        startActivity(Intent(this, login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_home)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java, false) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
        findViewById<View>(R.id.ll_nav_profile)?.setOnClickListener { navigateTo(SettingsActivity::class.java) }
        findViewById<TextView>(R.id.btn_view_all)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>, reorder: Boolean = true) {
        val intent = Intent(this, cls)
        if (reorder) intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun startRealTimeSync() {
        val userId = auth.currentUser?.uid ?: return
        val currentLocalCycle = sharedPreferences.getInt("userBudgetCycleDay", 1)

        listeners.add(firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    findViewById<TextView>(R.id.tv_welcome).text = "Hi ${snapshot.getString("firstName") ?: "User"}! 👋"
                    val cloudCycleDay = snapshot.getLong("budgetCycleDay")?.toInt() ?: currentLocalCycle
                    if (cloudCycleDay != currentLocalCycle) {
                        sharedPreferences.edit().putInt("userBudgetCycleDay", cloudCycleDay).apply()
                        refreshUI()
                    }
                }
            })

        listeners.add(firestore.collection("categories").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                categoriesMap = snapshot?.documents?.associate { it.id to (it.getString("name") ?: "Unknown") }?.toMutableMap() ?: mutableMapOf()
                categoryColorsMap = snapshot?.documents?.associate { it.id to (it.getString("color") ?: "#808080") }?.toMutableMap() ?: mutableMapOf()
                categoryNameColorsMap = snapshot?.documents?.associate { (it.getString("name") ?: "Unknown") to (it.getString("color") ?: "#808080") }?.toMutableMap() ?: mutableMapOf()
                refreshUI()
            })

        listeners.add(firestore.collection("income_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                allIncomes = snapshot?.documents ?: emptyList()
                refreshUI()
            })

        listeners.add(firestore.collection("expense_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                allExpenses = snapshot?.documents ?: emptyList()
                refreshUI()
            })

        listeners.add(firestore.collection("category_budgets").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                allBudgetDocs = snapshot?.documents ?: emptyList()
                refreshUI()
            })
    }

    private fun refreshUI() {
        val cycleDay = sharedPreferences.getInt("userBudgetCycleDay", 1)
        val cycleRange = BudgetCycleHelper.getCurrentCycleRange(cycleDay)
        val prevRange = BudgetCycleHelper.getPreviousCycleRange(cycleDay)
        
        val currentExpenses = allExpenses.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), cycleRange) }
        val currentIncomes = allIncomes.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), cycleRange) }
        val totalIncome = currentIncomes.sumOf { getAmountFromDoc(it) }
        val totalExpenses = currentExpenses.sumOf { getAmountFromDoc(it) }
        val balance = totalIncome - totalExpenses
        val savings = if (balance > 0) balance else 0.0

        val prevIncomes = allIncomes.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), prevRange) }.sumOf { getAmountFromDoc(it) }
        val prevExpenses = allExpenses.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), prevRange) }.sumOf { getAmountFromDoc(it) }
        val prevBalance = prevIncomes - prevExpenses
        val prevSavings = if (prevBalance > 0) prevBalance else 0.0

        findViewById<TextView>(R.id.tv_balance_amount).text = String.format(Locale.US, "R %,.2f", balance)
        findViewById<TextView>(R.id.tv_income_summary).text = String.format(Locale.US, "R %,.0f", totalIncome)
        findViewById<TextView>(R.id.tv_expenses_summary).text = String.format(Locale.US, "R %,.0f", totalExpenses)
        findViewById<TextView>(R.id.tv_savings_summary).text = String.format(Locale.US, "R %,.0f", savings)

        updateChangePercent(findViewById(R.id.tv_income_change), totalIncome, prevIncomes, true)
        updateChangePercent(findViewById(R.id.tv_expense_change), totalExpenses, prevExpenses, false)
        updateChangePercent(findViewById(R.id.tv_savings_change), savings, prevSavings, true)
        updateChangePercent(findViewById(R.id.tv_monthly_change), balance, prevBalance, true, " This Cycle")

        // Safe Today Calculation - Only include budgets for categories that exist
        val activeTotalBudgets = allBudgetDocs.filter { bDoc -> 
            val catId = bDoc.getString("categoryId") ?: ""
            categoriesMap.containsKey(catId)
        }.sumOf { bDoc ->
            val mGoal = (bDoc.get("maxGoal") as? Number)?.toDouble() ?: 0.0
            if (mGoal > 0) mGoal else (bDoc.get("budgetAmount") as? Number)?.toDouble() ?: 0.0
        }

        val daysLeft = BudgetCycleHelper.getDaysRemaining(cycleRange)
        val remainingBudget = activeTotalBudgets - totalExpenses
        val safeAmount = if (remainingBudget > 0 && daysLeft > 0) remainingBudget / daysLeft else 0.0
        findViewById<TextView>(R.id.tv_safe_amount).text = String.format(Locale.US, "R %,.2f Safe Today", safeAmount)

        val budgetPercent = if (activeTotalBudgets > 0) ((totalExpenses / activeTotalBudgets) * 100).toInt() else 0
        findViewById<TextView>(R.id.tv_budget_percent).text = "$budgetPercent%"
        findViewById<ProgressBar>(R.id.pb_spending).apply {
            progress = budgetPercent.coerceAtMost(100)
            progressTintList = android.content.res.ColorStateList.valueOf(
                if (budgetPercent > 100) Color.parseColor("#F44336") else Color.parseColor("#4CAF50")
            )
        }

        setupDonutChart(currentExpenses)
        updateRecentList(currentExpenses, currentIncomes)
    }

    private fun updateChangePercent(view: TextView, current: Double, prev: Double, higherIsBetter: Boolean, suffix: String = "") {
        if (prev == 0.0) {
            view.text = if (current > 0) "↑ New" else "---"
            view.setTextColor(Color.GRAY)
            return
        }
        val diff = ((current - prev) / prev) * 100
        val sign = if (diff >= 0) "↑" else "↓"
        view.text = String.format(Locale.US, "%s %.0f%%%s", sign, Math.abs(diff), suffix)
        val isGood = if (higherIsBetter) diff >= 0 else diff <= 0
        view.setTextColor(if (isGood) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
    }

    private fun setupDonutChart(cycleExpenses: List<DocumentSnapshot>) {
        val onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        
        if (cycleExpenses.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No expenses logged in this cycle")
            pieChart.setNoDataTextColor(onSurfaceColor)
            return
        }

        val entries = mutableListOf<PieEntry>()
        val chartColors = mutableListOf<Int>()
        val totals = mutableMapOf<String, Double>()
        val catIds = mutableMapOf<String, String>()

        cycleExpenses.forEach { doc ->
            val catId = doc.getString("categoryId") ?: ""
            val name = categoriesMap[catId] ?: "Other"
            totals[name] = (totals[name] ?: 0.0) + getAmountFromDoc(doc)
            catIds[name] = catId
        }

        val totalSum = totals.values.sum().toFloat()
        totals.forEach { (name, value) -> 
            val percentage = if (totalSum > 0) (value / totalSum * 100) else 0.0
            // Format to match the picture: "32,3 %" (one decimal and space)
            val percentLabel = String.format(Locale.getDefault(), "%.1f %%", percentage)
            
            // PieEntry(value, label, data)
            // label -> drawn inside (Percentage)
            // data -> drawn outside via formatter (Category Name)
            entries.add(PieEntry(value.toFloat(), percentLabel, name))
            
            val colorStr = categoryColorsMap[catIds[name]] ?: "#808080"
            chartColors.add(Color.parseColor(colorStr))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = chartColors
            setDrawValues(true)
            valueTextColor = onSurfaceColor // Category Names color (Outside)
            valueTextSize = 12f
            sliceSpace = 3f
            
            // Match the attached picture: Names outside (ValuePos), Percentages inside (XPos)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            valueLineColor = onSurfaceColor
        }
        
        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    // Return the category name (stored in data) for the outside label
                    return pieEntry?.data as? String ?: ""
                }
            })
        }
        
        pieChart.apply {
            data = pieData
            setUsePercentValues(false) // Manually formatted percentages into entry labels
            isDrawHoleEnabled = true
            holeRadius = 60f
            transparentCircleRadius = 65f
            setHoleColor(Color.TRANSPARENT)
            centerText = "Personal\nSpending"
            setCenterTextSize(14f)
            setCenterTextColor(onSurfaceColor)
            
            setEntryLabelColor(Color.WHITE) // Percentages color (Inside)
            setEntryLabelTextSize(12f)
            setDrawEntryLabels(true) // Enable percentages inside
            
            description.isEnabled = false
            legend.isEnabled = false
            setExtraOffsets(45f, 10f, 45f, 10f) // Ample space for names around chart
            animateY(1200)
            invalidate()
        }
    }

    private fun getAmountFromDoc(doc: DocumentSnapshot): Double {
        val amt = doc.get("amount")
        return when (amt) {
            is Number -> amt.toDouble()
            is String -> amt.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun updateRecentList(expenses: List<DocumentSnapshot>, incomes: List<DocumentSnapshot>) {
        val uiList = mutableListOf<TransactionUI>()
        expenses.forEach { doc ->
            val catId = doc.getString("categoryId") ?: ""
            uiList.add(TransactionUI(doc.id, doc.getString("description") ?: "Expense", categoriesMap[catId] ?: "Unknown", 
                getAmountFromDoc(doc), doc.getString("date") ?: "", doc.getString("startTime") ?: "", false, 
                doc.getString("photoPath") != null, doc.getString("photoPath"), categoryColorsMap[catId] ?: "#808080"))
        }
        incomes.forEach { doc ->
             val source = doc.getString("source") ?: "Income"
             uiList.add(TransactionUI(doc.id, doc.getString("source") ?: "Income", "Income",
                getAmountFromDoc(doc), doc.getString("date") ?: "", "", true, categoryColor = categoryNameColorsMap[source] ?: "#4CAF50"))
        }
        val dashboardList = uiList.sortedByDescending { it.date }.take(3)
        rvRecent.adapter = TransactionAdapter(dashboardList, { t -> deleteTransaction(t) }, { p -> showFullPhoto(p) })
    }

    private fun deleteTransaction(transaction: TransactionUI) {
        val collection = if (transaction.isIncome) "income_entries" else "expense_entries"
        firestore.collection(collection).document(transaction.id).delete()
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

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }
}
