package com.example.cyber_budget

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        db = AppDatabase.getDatabase(this)
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        rvNotifications = findViewById(R.id.rv_notifications)
        tvEmpty = findViewById(R.id.tv_empty)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        loadNotifications()
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val budgets = db.categoryBudgetDao().getBudgetsForMonth(userId, currentMonth)
            val categories = db.categoryDao().getCategoriesForUser(userId)
            
            val notificationList = mutableListOf<NotificationItem>()

            // 1. Generate Budget Alerts
            for (budget in budgets) {
                val category = categories.find { it.id == budget.categoryId }
                val categoryName = category?.name ?: "Unknown"
                
                // Get expenses for this specific category and month
                val expenses = db.expenseEntryDao().getExpensesByPeriod(
                    userId, 
                    "$currentMonth-01", 
                    "$currentMonth-31"
                ).filter { it.categoryId == budget.categoryId }
                
                val spent = expenses.sumOf { it.amount }
                val percentage = if (budget.budgetAmount > 0) (spent / budget.budgetAmount) * 100 else 0.0

                if (percentage >= 100) {
                    notificationList.add(
                        NotificationItem(
                            "Budget Exceeded",
                            "You have exceeded your budget for '$categoryName'. Spent: R${String.format(Locale.US, "%.2f", spent)} / R${String.format(Locale.US, "%.2f", budget.budgetAmount)}",
                            true
                        )
                    )
                } else if (percentage >= 80) {
                    notificationList.add(
                        NotificationItem(
                            "Budget Warning",
                            "You have used ${percentage.toInt()}% of your budget for '$categoryName'.",
                            true
                        )
                    )
                }
            }

            // 2. Add General Reminders
            notificationList.add(
                NotificationItem(
                    "Daily Reminder",
                    "Don't forget to log your daily expenses to stay on track!",
                    false
                )
            )

            if (notificationList.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvNotifications.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvNotifications.visibility = View.VISIBLE
                rvNotifications.adapter = NotificationAdapter(notificationList)
            }
        }
    }
}
