package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private lateinit var llBudgetList: LinearLayout
    private lateinit var llAlertsList: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (!isSessionValid()) {
            logoutUser()
            return
        }

        setContentView(R.layout.activity_summary)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)
        
        userId = sharedPreferences.getInt("userId", -1)

        llBudgetList = findViewById(R.id.ll_budget_list)
        llAlertsList = findViewById(R.id.ll_alerts_list)

        findViewById<Button>(R.id.btn_adjust_budget).setOnClickListener {
            showAdjustBudgetDialog()
        }

        setupNavbar()
        loadSummaryData()
    }

    private fun isSessionValid(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (!isLoggedIn) return false
        
        val lastActive = sharedPreferences.getLong("lastActive", 0L)
        val currentTime = System.currentTimeMillis()
        val sessionDuration = 30 * 60 * 1000 
        
        return (currentTime - lastActive) < sessionDuration
    }

    private fun logoutUser() {
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadSummaryData() {
        lifecycleScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val categories = db.categoryDao().getCategoriesForUser(userId)
            val budgets = db.categoryBudgetDao().getBudgetsForMonth(userId, currentMonth)
            val expenses = db.expenseEntryDao().getExpensesByPeriod(userId, "$currentMonth-01", "$currentMonth-31")

            llBudgetList.removeAllViews()
            llAlertsList.removeAllViews()

            // Add Header back
            val header = TextView(this@SummaryActivity).apply {
                text = "Budget Progress"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#2E4B94"))
                setPadding(0, 0, 0, 16)
            }
            llBudgetList.addView(header)

            val alertsHeader = TextView(this@SummaryActivity).apply {
                text = "Budget Alerts"
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.RED)
                setPadding(0, 0, 0, 8)
            }
            llAlertsList.addView(alertsHeader)

            categories.forEach { category ->
                val spent = expenses.filter { it.categoryId == category.id }.sumOf { it.amount }
                val budget = budgets.find { it.categoryId == category.id }?.budgetAmount ?: 0.0

                if (budget > 0) {
                    addBudgetProgressView(category.name, spent, budget)
                    if (spent > budget) {
                        addAlertView(category.name, "Over Budget")
                    } else if (spent > budget * 0.8) {
                        addAlertView(category.name, "Near Limit")
                    }
                }
            }
        }
    }

    private fun addBudgetProgressView(name: String, spent: Double, budget: Double) {
        val view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, llBudgetList, false)
        val tvLabel = view.findViewById<TextView>(android.R.id.text1)
        val tvProgress = view.findViewById<TextView>(android.R.id.text2)
        
        val percent = (spent / budget * 100).toInt()
        tvLabel.text = "$name ($percent%)"
        tvProgress.text = "Spent: R${String.format(Locale.US, "%.2f", spent)} / Budget: R${String.format(Locale.US, "%.2f", budget)}"
        
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = percent
            progressTintList = android.content.res.ColorStateList.valueOf(if (percent > 90) Color.RED else if (percent > 70) Color.YELLOW else Color.GREEN)
        }
        
        llBudgetList.addView(view)
        llBudgetList.addView(progressBar)
    }

    private fun addAlertView(name: String, status: String) {
        val tv = TextView(this).apply {
            text = "• $name - $status"
            setPadding(8, 4, 8, 4)
        }
        llAlertsList.addView(tv)
    }

    private fun showAdjustBudgetDialog() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getCategoriesForUser(userId)
            val names = categories.map { it.name }.toTypedArray()

            if (names.isEmpty()) {
                Toast.makeText(this@SummaryActivity, "Create categories first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            var selectedCategoryIndex = 0
            val builder = AlertDialog.Builder(this@SummaryActivity)
            builder.setTitle("Select Category to Adjust Budget")
            
            builder.setSingleChoiceItems(names, 0) { _, which ->
                selectedCategoryIndex = which
            }

            builder.setPositiveButton("Next") { _, _ ->
                showSetAmountDialog(categories[selectedCategoryIndex])
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
    }

    private fun showSetAmountDialog(category: Category) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter Budget Amount"

        AlertDialog.Builder(this)
            .setTitle("Set Budget for ${category.name}")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    saveBudget(category.id, amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBudget(categoryId: Int, amount: Double) {
        lifecycleScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            // Check if a budget already exists for this category/month
            val existing = db.categoryBudgetDao().getBudgetForCategory(userId, categoryId, currentMonth)
            
            if (existing != null) {
                // UPDATE existing
                val updated = existing.copy(budgetAmount = amount)
                db.categoryBudgetDao().updateBudget(updated)
            } else {
                // INSERT new
                val budget = CategoryBudget(userId = userId, categoryId = categoryId, month = currentMonth, budgetAmount = amount)
                db.categoryBudgetDao().insertBudget(budget)
            }

            Toast.makeText(this@SummaryActivity, "Budget Updated", Toast.LENGTH_SHORT).show()
            loadSummaryData()
        }
    }

    private fun setupNavbar() {
        findViewById<ImageView>(R.id.nav_dash)?.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_analytics)?.setOnClickListener { /* Already here */ }
        findViewById<ImageView>(R.id.nav_plus)?.setOnClickListener { startActivity(Intent(this, AddTransactionActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_card)?.setOnClickListener { startActivity(Intent(this, TransactionsActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_settings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        if (!isSessionValid()) {
            logoutUser()
        }
    }
}
