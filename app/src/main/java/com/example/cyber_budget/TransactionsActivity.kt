package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class TransactionsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private lateinit var rvTransactions: RecyclerView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (!isSessionValid()) {
            logoutUser()
            return
        }

        setContentView(R.layout.activity_transactions)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)
        
        userId = sharedPreferences.getInt("userId", -1)

        rvTransactions = findViewById(R.id.rv_transactions)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = TransactionAdapter(emptyList()) {}

        setupNavbar()
        loadTransactions()
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

    private fun loadTransactions() {
        lifecycleScope.launch {
            val expenses = db.expenseEntryDao().getExpensesByPeriod(userId, "0000-00-00", "9999-12-31")
            val income = db.incomeEntryDao().getIncomeForUser(userId)
            val categories = db.categoryDao().getCategoriesForUser(userId)

            val uiList = mutableListOf<TransactionUI>()
            
            expenses.forEach { e ->
                val catName = categories.find { it.id == e.categoryId }?.name ?: "Unknown"
                uiList.add(TransactionUI(e.id, e.description, catName, e.amount, e.date, e.startTime, false))
            }
            
            income.forEach { i ->
                uiList.add(TransactionUI(i.id, i.source, "Income", i.amount, i.date, "", true))
            }

            val sortedList = uiList.sortedByDescending { it.date }
            
            rvTransactions.adapter = TransactionAdapter(sortedList) { transaction ->
                showDeleteConfirmation(transaction)
            }
        }
    }

    private fun showDeleteConfirmation(transaction: TransactionUI) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete '${transaction.description}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    if (transaction.isIncome) {
                        val incomeList = db.incomeEntryDao().getIncomeForUser(userId)
                        val income = incomeList.find { it.id == transaction.id }
                        if (income != null) db.incomeEntryDao().deleteIncome(income)
                    } else {
                        val expense = db.expenseEntryDao().getExpenseById(transaction.id)
                        if (expense != null) db.expenseEntryDao().deleteExpense(expense)
                    }
                    Toast.makeText(this@TransactionsActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    loadTransactions() // Refresh
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupNavbar() {
        findViewById<ImageView>(R.id.nav_dash)?.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_analytics)?.setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_plus)?.setOnClickListener { startActivity(Intent(this, AddTransactionActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_card)?.setOnClickListener { /* Already here */ }
        findViewById<ImageView>(R.id.nav_settings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        if (!isSessionValid()) {
            logoutUser()
        }
    }
}
