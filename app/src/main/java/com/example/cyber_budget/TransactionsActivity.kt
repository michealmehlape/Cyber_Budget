package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity for viewing and managing all historical transactions.
 * Features:
 * - Combined list of Income and Expense records.
 * - Image attachment viewing logic.
 * - Transaction deletion with associated file cleanup.
 */
class TransactionsActivity : AppCompatActivity() {

    private val TAG = "Transactions_Debug"
    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private lateinit var rvTransactions: RecyclerView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (!isSessionValid()) {
            Log.w(TAG, "Session expired or invalid. Redirecting to login.")
            logoutUser()
            return
        }

        setContentView(R.layout.activity_transactions)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)
        
        userId = sharedPreferences.getInt("userId", -1)

        rvTransactions = findViewById(R.id.rv_transactions)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        
        // Initialize with empty list to prevent "No adapter attached" warning
        rvTransactions.adapter = TransactionAdapter(emptyList(), {}, {})

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

    /**
     * Fetches transactions from the database and updates the UI.
     * Uses coroutines to perform database operations off the main thread.
     */
    private fun loadTransactions() {
        lifecycleScope.launch {
            Log.d(TAG, "Loading all transactions for user: $userId")
            val expenses = db.expenseEntryDao().getExpensesByPeriod(userId, "0000-00-00", "9999-12-31")
            val income = db.incomeEntryDao().getIncomeForUser(userId)
            val categories = db.categoryDao().getCategoriesForUser(userId)

            val uiList = mutableListOf<TransactionUI>()
            
            // Map raw database entities to UI model
            expenses.forEach { e ->
                val catName = categories.find { it.id == e.categoryId }?.name ?: "Unknown"
                uiList.add(TransactionUI(
                    id = e.id,
                    description = e.description,
                    categoryName = catName,
                    amount = e.amount,
                    date = e.date,
                    time = e.startTime,
                    isIncome = false,
                    hasPhoto = e.photoPath != null,
                    photoPath = e.photoPath
                ))
            }
            
            income.forEach { i ->
                uiList.add(TransactionUI(
                    id = i.id,
                    description = i.source,
                    categoryName = "Income",
                    amount = i.amount,
                    date = i.date,
                    time = "",
                    isIncome = true
                ))
            }

            // Sort newest first
            val sortedList = uiList.sortedByDescending { it.date }
            Log.i(TAG, "Displaying ${sortedList.size} transactions")
            
            rvTransactions.adapter = TransactionAdapter(sortedList, 
                onDeleteClick = { transaction -> showDeleteConfirmation(transaction) },
                onViewPhotoClick = { path -> showPhotoDialog(path) }
            )
        }
    }

    /**
     * Displays a dialog with the receipt image.
     * Reference: https://developer.android.com/reference/android/app/AlertDialog
     */
    private fun showPhotoDialog(path: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_photo, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.iv_full_photo)
        val file = File(path)
        if (file.exists()) {
            Log.v(TAG, "Opening image file: $path")
            imageView.setImageURI(Uri.fromFile(file))
        } else {
            Log.e(TAG, "Image file not found at: $path")
            Toast.makeText(this, "Photo not found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    /**
     * Confirms deletion and handles physical file cleanup if a photo is attached.
     */
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
                        if (expense != null) {
                            // Security check: Delete local storage image file to maintain privacy
                            expense.photoPath?.let { path ->
                                val deleted = File(path).delete()
                                Log.d(TAG, "Internal storage cleanup: File deleted = $deleted")
                            }
                            db.expenseEntryDao().deleteExpense(expense)
                        }
                    }
                    Log.i(TAG, "Transaction deleted successfully")
                    Toast.makeText(this@TransactionsActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    loadTransactions() // Refresh the view
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
