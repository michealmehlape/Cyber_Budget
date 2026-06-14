package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

/**
 * SummaryActivity: Budget Analysis screen.
 * Restored: Progress budget list with cycle-over-cycle comparisons.
 * Fixed: Badge explanations cutoff and Bar Chart color synchronization.
 */
class SummaryActivity : AppCompatActivity() {

    private val TAG = "SummaryActivity_Log"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var llBudgetList: LinearLayout
    private lateinit var llAlertsList: LinearLayout
    private lateinit var llBadges: LinearLayout
    private lateinit var barChart: BarChart
    private lateinit var tvAiInsight: TextView
    private lateinit var sharedPreferences: SharedPreferences
    
    private var categories = listOf<DocumentSnapshot>()
    private var budgets = listOf<DocumentSnapshot>()
    private var allExpenses = listOf<DocumentSnapshot>()
    private var allIncomes = listOf<DocumentSnapshot>()
    private var archivedBadges = listOf<String>()
    
    private var isInitialLoadComplete = false
    private val listeners = mutableListOf<ListenerRegistration>()

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
        setContentView(R.layout.activity_summary)
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

        llBudgetList = findViewById(R.id.ll_budget_list)
        llAlertsList = findViewById(R.id.ll_alerts_list)
        llBadges = findViewById(R.id.ll_badges)
        barChart = findViewById(R.id.barChart)
        tvAiInsight = findViewById(R.id.tv_ai_insight)

        findViewById<Button>(R.id.btn_adjust_budget).setOnClickListener { showAdjustBudgetDialog() }

        setupNavbar()
        startDataObserving()
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_insights)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java, false) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
        findViewById<View>(R.id.ll_nav_profile)?.setOnClickListener { navigateTo(SettingsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>, useReorder: Boolean = true) {
        val intent = Intent(this, cls)
        if (useReorder) intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun startDataObserving() {
        val userId = auth.currentUser?.uid ?: return
        val currentLocalCycle = sharedPreferences.getInt("userBudgetCycleDay", 1)

        listeners.add(firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    archivedBadges = (snapshot.get("earnedBadges") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val cloudCycleDay = snapshot.getLong("budgetCycleDay")?.toInt() ?: currentLocalCycle
                    if (cloudCycleDay != currentLocalCycle) {
                        sharedPreferences.edit().putInt("userBudgetCycleDay", cloudCycleDay).apply()
                    }
                    refreshUI()
                }
            })

        listeners.add(firestore.collection("categories").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                categories = snapshot?.documents ?: emptyList()
                isInitialLoadComplete = true
                refreshUI()
            })

        listeners.add(firestore.collection("category_budgets").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                budgets = snapshot?.documents ?: emptyList()
                refreshUI()
            })

        listeners.add(firestore.collection("expense_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                allExpenses = snapshot?.documents ?: emptyList()
                refreshUI()
            })

        listeners.add(firestore.collection("income_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                allIncomes = snapshot?.documents ?: emptyList()
                refreshUI()
            })
    }

    private fun refreshUI() {
        llBudgetList.removeAllViews()
        llAlertsList.removeAllViews()
        llBadges.removeAllViews()

        if (!isInitialLoadComplete) {
            addAlertView("Status", "Analyzing your data...", Color.GRAY)
            return
        }

        val cycleDay = sharedPreferences.getInt("userBudgetCycleDay", 1)
        val cycleRange = BudgetCycleHelper.getCurrentCycleRange(cycleDay)
        val prevRange = BudgetCycleHelper.getPreviousCycleRange(cycleDay)
        
        val cycleExpenses = allExpenses.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), cycleRange) }
        val prevExpenses = allExpenses.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), prevRange) }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val chartColors = mutableListOf<Int>()
        
        var onTrackCount = 0
        var totalOverBudget = 0
        var chartIndex = 0f
        var hasActiveBudgets = false

        val activeCatIds = categories.map { it.id }.toSet()

        categories.forEach { doc ->
            val catId = doc.id
            val catName = doc.getString("name") ?: "Other"
            val catColor = doc.getString("color") ?: "#808080"
            
            val spent = cycleExpenses.filter { it.getString("categoryId") == catId }.sumOf { eDoc -> getAmountFromField(eDoc, "amount") }
            val spentPrev = prevExpenses.filter { it.getString("categoryId") == catId }.sumOf { pDoc -> getAmountFromField(pDoc, "amount") }
            
            val budgetDoc = budgets.find { it.getString("categoryId") == catId }
            val maxGoal = getAmountFromField(budgetDoc, "maxGoal").let { mGoal ->
                if (mGoal > 0) mGoal else getAmountFromField(budgetDoc, "budgetAmount")
            }
            val minGoal = getAmountFromField(budgetDoc, "minGoal")

            if (maxGoal > 0) {
                hasActiveBudgets = true
                addBudgetProgressView(catName, spent, spentPrev, minGoal, maxGoal, catColor)
                
                entries.add(BarEntry(chartIndex, spent.toFloat()))
                labels.add(catName)
                try {
                    chartColors.add(Color.parseColor(catColor))
                } catch (e: Exception) {
                    chartColors.add(Color.GRAY)
                }
                chartIndex += 1f
                
                if (spent in minGoal..maxGoal) onTrackCount++
                
                if (spent > maxGoal) {
                    totalOverBudget++
                    addAlertView(catName, "Over Budget!", Color.parseColor("#FF5252"))
                } else if (spent >= (maxGoal * 0.8)) {
                    addAlertView(catName, "Approaching Limit (80%+)", Color.parseColor("#FF9800"))
                }
            }
        }

        if (hasActiveBudgets) {
            if (totalOverBudget == 0 && llAlertsList.childCount == 0) {
                addAlertView("Status", "🎉 Everything is on track! Keep it up.", Color.parseColor("#4CAF50"))
            }
        } else if (categories.isNotEmpty()) {
            addAlertView("Budget", "No budget goals set for this cycle. Tap 'Adjust' below to start.", Color.GRAY)
        }
        
        if (entries.isNotEmpty()) setupChart(entries, labels, chartColors)
        
        archivedBadges.forEach { badgeId ->
            when (badgeId) {
                "activelogger" -> addBadge(R.drawable.activeloggerbadge, "Active Logger", "Consistency is key! You earned this by logging 10 or more expenses. It shows your dedication to tracking every cent of your budget cycle.")
                "budgetpro" -> addBadge(R.drawable.budgetprobadge, "Budget Pro", "Master of discipline! You've successfully managed your cycle with zero categories going over budget. Great job sticking to your plan!")
                "financeguru" -> addBadge(R.drawable.financegurubadge, "Finance Guru", "Expert level budgeting! You've maintained strict control over at least 3 different spending categories simultaneously without overspending.")
                "saverhero" -> addBadge(R.drawable.saverherobadge, "Saver Hero", "Wealth builder! Your total income for this cycle was higher than your total expenses, meaning you've successfully saved money.")
            }
        }

        checkAndArchiveNewBadges(onTrackCount, totalOverBudget, cycleExpenses, allIncomes) 
        
        val totalSpent = cycleExpenses.sumOf { getAmountFromField(it, "amount") }
        val totalSpentPrev = prevExpenses.sumOf { getAmountFromField(it, "amount") }
        val totalBudgetAmount = budgets.filter { activeCatIds.contains(it.getString("categoryId")) }.sumOf { bDoc ->
            val mGoal = getAmountFromField(bDoc, "maxGoal")
            if (mGoal > 0) mGoal else getAmountFromField(bDoc, "budgetAmount")
        }
        
        val daysLeft = BudgetCycleHelper.getDaysRemaining(cycleRange)
        tvAiInsight.text = generateSmartInsight(totalSpent, totalSpentPrev, totalOverBudget, totalBudgetAmount, daysLeft)
    }

    private fun generateSmartInsight(current: Double, previous: Double, overCount: Int, totalBudget: Double, daysLeft: Int): String {
        if (current == 0.0) return "Start logging your cycle expenses to see personalized AI insights here."
        var msg = if (overCount > 0) "⚠️ You're over budget in $overCount categories. " else "✅ You're maintaining your cycle well. "
        if (totalBudget > current && daysLeft > 0) {
            val dailyRemaining = (totalBudget - current) / daysLeft
            msg += "You can spend R%,.2f per day for the remaining $daysLeft days.".format(dailyRemaining)
        }
        if (previous > 0) {
            val diff = ((current - previous) / previous) * 100
            msg += "\n\n📊 Comparison: You've spent %.0f%% %s than last cycle.".format(Math.abs(diff), if (diff > 0) "more" else "less")
        }
        return msg
    }

    private fun addAlertView(category: String, message: String, color: Int) {
        val tv = TextView(this).apply {
            text = if (category == "Status" || category == "Budget") message else "• $category: $message"
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, 10, 0, 10)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        llAlertsList.addView(tv)
    }

    private fun getAmountFromField(doc: DocumentSnapshot?, field: String): Double {
        if (doc == null) return 0.0
        val value = doc.get(field)
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun checkAndArchiveNewBadges(onTrack: Int, over: Int, cycleExpenses: List<DocumentSnapshot>, allIncomes: List<DocumentSnapshot>) {
        val earnedNow = mutableListOf<String>()
        if (allExpenses.size >= 10) earnedNow.add("activelogger")
        if (onTrack > 0 && over == 0) earnedNow.add("budgetpro")
        if (onTrack >= 3 && over == 0) earnedNow.add("financeguru")
        val totalSpent = cycleExpenses.sumOf { getAmountFromField(it, "amount") }
        val cycleRange = BudgetCycleHelper.getCurrentCycleRange(sharedPreferences.getInt("userBudgetCycleDay", 1))
        val totalIncome = allIncomes.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), cycleRange) }.sumOf { getAmountFromField(it, "amount") }
        if (totalIncome > 0 && totalIncome > totalSpent) earnedNow.add("saverhero")
        if (earnedNow.isNotEmpty()) {
            val userId = auth.currentUser?.uid ?: return
            val updated = (archivedBadges + earnedNow).distinct()
            if (updated.size > archivedBadges.size) firestore.collection("users").document(userId).update("earnedBadges", updated)
        }
    }

    private fun setupChart(entries: List<BarEntry>, labels: List<String>, colors: List<Int>) {
        val dataSet = BarDataSet(entries, "Spending (R)")
        dataSet.colors = colors
        dataSet.valueTextColor = if (isDarkMode()) Color.WHITE else Color.BLACK
        dataSet.valueTextSize = 10f
        dataSet.setDrawValues(true)

        barChart.data = BarData(dataSet)
        barChart.setExtraOffsets(10f, 10f, 10f, 80f)
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = if (isDarkMode()) Color.WHITE else Color.BLACK
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size
        xAxis.labelRotationAngle = -45f
        xAxis.setAvoidFirstLastClipping(false)
        barChart.axisLeft.textColor = if (isDarkMode()) Color.WHITE else Color.BLACK
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun addBadge(drawableId: Int, title: String, description: String) {
        val badgeView = ImageView(this).apply {
            setImageResource(drawableId)
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, resources.displayMetrics).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = 12 }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { showBadgeExplanation(drawable, title, description) }
        }
        llBadges.addView(badgeView)
    }

    private fun showBadgeExplanation(drawable: Drawable?, title: String, description: String) {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(50, 60, 50, 60)
        }

        val imageSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160f, resources.displayMetrics).toInt()
        val fullIv = ImageView(this).apply { 
            setImageDrawable(drawable)
            layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply { bottomMargin = 32 } 
        }

        val tvTitle = TextView(this).apply { 
            text = title; setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0,0,0,16) 
        }

        val tvDesc = TextView(this).apply { 
            text = description; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics), 1.1f)
        }

        container.addView(fullIv); container.addView(tvTitle); container.addView(tvDesc)
        scrollView.addView(container)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_NoActionBar)
            .setView(scrollView).setPositiveButton("Awesome!", null).create()
        
        dialog.show()
        dialog.window?.let { 
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            it.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun addBudgetProgressView(name: String, spent: Double, spentPrev: Double, min: Double, max: Double, color: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_category_budget, llBudgetList, false)
        val tvName = view.findViewById<TextView>(R.id.tv_cat_name)
        val tvStatus = view.findViewById<TextView>(R.id.tv_status)
        val tvSpentGoal = view.findViewById<TextView>(R.id.tv_spent_goal)
        val pb = view.findViewById<ProgressBar>(R.id.pb_budget)

        tvName.text = name
        try { tvName.setTextColor(Color.parseColor(color)) } catch (e: Exception) {}
        
        val growthText = if (spentPrev > 0) {
            val change = ((spent - spentPrev) / spentPrev * 100).toInt()
            val sign = if (change >= 0) "↑" else "↓"
            val statusColor = if (change > 0) "#F44336" else "#4CAF50"
            tvStatus.text = "$sign ${Math.abs(change)}%"
            tvStatus.setTextColor(Color.parseColor(statusColor))
            " (Prev: R%,.0f)".format(Locale.US, spentPrev)
        } else {
            tvStatus.text = "New"
            tvStatus.setTextColor(Color.GRAY)
            ""
        }

        tvSpentGoal.text = String.format(Locale.US, "Spent: R%,.2f / Goal: R%,.0f%s", spent, max, growthText)
        val percent = if (max > 0) ((spent / max) * 100).toInt() else 0
        pb.progress = percent.coerceAtMost(100)
        pb.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(if (spent > max) "#F44336" else if (spent >= max * 0.8) "#FF9800" else "#4CAF50"))
        llBudgetList.addView(view)
    }

    private fun isDarkMode() = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun showAdjustBudgetDialog() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("categories").whereEqualTo("userId", userId).get().addOnSuccessListener { docs ->
            val names = docs.documents.map { it.getString("name") ?: "Unknown" }.toTypedArray()
            if (names.isEmpty()) return@addOnSuccessListener
            var selected = 0
            AlertDialog.Builder(this).setTitle("Select Category").setSingleChoiceItems(names, 0) { _, i -> selected = i }
                .setPositiveButton("Set Goals") { _, _ -> showSetGoalDialog(docs.documents[selected].id, names[selected]) }.show()
        }
    }

    private fun showSetGoalDialog(catId: String, name: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_set_goals, null)
        val etMin = view.findViewById<EditText>(R.id.et_min_goal)
        val etMax = view.findViewById<EditText>(R.id.et_max_goal)
        AlertDialog.Builder(this).setTitle("Goals for $name").setView(view)
            .setPositiveButton("Save") { _, _ ->
                val min = etMin.text.toString().toDoubleOrNull() ?: 0.0
                val max = etMax.text.toString().toDoubleOrNull() ?: 0.0
                saveGoals(catId, min, max)
            }.show()
    }

    private fun saveGoals(catId: String, min: Double, max: Double) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("category_budgets").whereEqualTo("userId", userId).whereEqualTo("categoryId", catId).get().addOnSuccessListener { docs ->
            val data = hashMapOf("userId" to userId, "categoryId" to catId, "minGoal" to min, "maxGoal" to max, "budgetAmount" to max)
            if (docs.isEmpty) firestore.collection("category_budgets").add(data)
            else firestore.collection("category_budgets").document(docs.documents[0].id).set(data)
            Toast.makeText(this, "Budget Goal Saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }
}
