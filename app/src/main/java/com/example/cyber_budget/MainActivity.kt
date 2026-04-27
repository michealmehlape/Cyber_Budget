package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rvRecent: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        
        // --- CHECK SESSION VALIDITY ---
        if (!isSessionValid()) {
            logoutUser()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userId = sharedPreferences.getInt("userId", -1)
        val firstName = sharedPreferences.getString("userFirstName", "User")
        findViewById<TextView>(R.id.tv_welcome).text = "Welcome back $firstName!"

        rvRecent = findViewById(R.id.rv_recent_activity)
        rvRecent.layoutManager = LinearLayoutManager(this)

        setupNavbar()
    }

    private fun isSessionValid(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (!isLoggedIn) return false
        
        val lastActive = sharedPreferences.getLong("lastActive", 0L)
        val currentTime = System.currentTimeMillis()
        val sessionDuration = 30 * 60 * 1000 // 30 minutes
        
        return (currentTime - lastActive) < sessionDuration
    }

    private fun logoutUser() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.apply()
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupNavbar() {
        findViewById<ImageView>(R.id.nav_dash)?.setOnClickListener { /* Already here */ }
        findViewById<ImageView>(R.id.nav_analytics)?.setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_plus)?.setOnClickListener { startActivity(Intent(this, AddTransactionActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_card)?.setOnClickListener { startActivity(Intent(this, TransactionsActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_settings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<TextView>(R.id.btn_view_all)?.setOnClickListener {
             startActivity(Intent(this, TransactionsActivity::class.java))
        }
    }

    private fun updateDashboard(userId: Int) {
        lifecycleScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            
            // 1. Fetch Data
            val totalIncome = db.incomeEntryDao().getTotalIncomeForMonth(userId, currentMonth) ?: 0.0
            val totalExpenses = db.expenseEntryDao().getTotalExpensesForMonth(userId, currentMonth) ?: 0.0
            val balance = totalIncome - totalExpenses
            
            val recentExpenses = db.expenseEntryDao().getRecentExpenses(userId)
            val recentIncome = db.incomeEntryDao().getIncomeForUser(userId).take(3)
            val categories = db.categoryDao().getCategoriesForUser(userId)

            // 2. Combine and Sort for Recent Activity
            val uiList = mutableListOf<TransactionUI>()
            recentExpenses.forEach { e ->
                val catName = categories.find { it.id == e.categoryId }?.name ?: "Unknown"
                uiList.add(TransactionUI(e.id, e.description, catName, e.amount, e.date, e.startTime, false))
            }
            recentIncome.forEach { i ->
                uiList.add(TransactionUI(i.id, i.source, "Income", i.amount, i.date, "", true))
            }
            val dashboardList = uiList.sortedByDescending { it.date }.take(3)

            // 3. Update UI
            findViewById<TextView>(R.id.tv_balance_amount).text = "R ${String.format(Locale.US, "%.2f", balance)}"
            
            // Remaining Calculation
            val totalBudgets = db.categoryBudgetDao().getBudgetsForMonth(userId, currentMonth).sumOf { it.budgetAmount }
            val displayGoal = if (totalBudgets > 0) totalBudgets else 0.0
            val remaining = displayGoal - totalExpenses
            
            val tvMonthlyChange = findViewById<TextView>(R.id.tv_monthly_change)
            if (displayGoal > 0) {
                if (remaining >= 0) {
                    tvMonthlyChange.text = "R ${String.format(Locale.US, "%.2f", remaining)} Remaining"
                    tvMonthlyChange.setTextColor(Color.parseColor("#90EE90"))
                } else {
                    tvMonthlyChange.text = "R ${String.format(Locale.US, "%.2f", -remaining)} Over Budget"
                    tvMonthlyChange.setTextColor(Color.RED)
                }
            } else {
                tvMonthlyChange.text = "Set a budget in Summary to track"
                tvMonthlyChange.setTextColor(Color.LTGRAY)
            }

            rvRecent.adapter = TransactionAdapter(dashboardList) { transaction ->
                showDeleteConfirmation(transaction, userId)
            }

            // Spending Summary logic updated to use sum of Category Budgets
            findViewById<TextView>(R.id.tv_spending_summary).text = "R ${String.format(Locale.US, "%.0f", totalExpenses)} / R ${String.format(Locale.US, "%.0f", displayGoal)}"
            findViewById<ProgressBar>(R.id.pb_spending).progress = if (displayGoal > 0) ((totalExpenses / displayGoal) * 100).toInt() else 0
            
            findViewById<TextView>(R.id.tv_income_summary).text = "R ${String.format(Locale.US, "%.0f", totalIncome)}"
        }
    }

    private fun showDeleteConfirmation(transaction: TransactionUI, userId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Remove '${transaction.description}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    if (transaction.isIncome) {
                        val items = db.incomeEntryDao().getIncomeForUser(userId)
                        val item = items.find { it.id == transaction.id }
                        if (item != null) db.incomeEntryDao().deleteIncome(item)
                    } else {
                        val item = db.expenseEntryDao().getExpenseById(transaction.id)
                        if (item != null) db.expenseEntryDao().deleteExpense(item)
                    }
                    updateDashboard(userId) // Refresh
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (!isSessionValid()) {
            logoutUser()
            return
        }
        val userId = sharedPreferences.getInt("userId", -1)
        if (userId != -1) updateDashboard(userId)
    }
}
