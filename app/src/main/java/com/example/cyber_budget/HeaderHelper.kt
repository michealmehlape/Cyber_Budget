package com.example.cyber_budget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object HeaderHelper {

    fun setupHeader(activity: Activity, db: AppDatabase) {
        val sharedPreferences = activity.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        
        // Update Session Activity
        sharedPreferences.edit().putLong("lastActive", System.currentTimeMillis()).apply()

        val profileIcon = activity.findViewById<View>(R.id.cv_profile_container)
        val notifContainer = activity.findViewById<View>(R.id.header_fl_notification)
        val badge = activity.findViewById<TextView>(R.id.header_notification_badge)

        profileIcon?.setOnClickListener {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
        }

        notifContainer?.setOnClickListener {
            activity.startActivity(Intent(activity, NotificationsActivity::class.java))
        }

        // Handle back buttons if they exist in the layout (as per user request)
        activity.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            activity.startActivity(Intent(activity, NotificationsActivity::class.java))
        }

        if (activity is LifecycleOwner) {
            activity.lifecycleScope.launch {
                updateNotificationBadge(userId, db, badge)
            }
        }
    }

    private suspend fun updateNotificationBadge(userId: Int, db: AppDatabase, badge: TextView?) {
        if (badge == null || userId == -1) return

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val budgets = db.categoryBudgetDao().getBudgetsForMonth(userId, currentMonth)
        
        var count = 0
        // We only count actual budget alerts now. 
        // Daily Reminder is always present in the notifications page but doesn't trigger a permanent badge.

        for (budget in budgets) {
            val spent = db.expenseEntryDao().getExpensesByPeriod(
                userId, 
                "$currentMonth-01", 
                "$currentMonth-31"
            ).filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
            
            val percentage = if (budget.budgetAmount > 0) (spent / budget.budgetAmount) * 100 else 0.0
            
            // Count if spent >= 80% of budget
            if (spent > 0 && percentage >= 80) {
                count++
            }
        }

        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}
