package com.example.cyber_budget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import java.util.*

/**
 * Shared utility to manage the consistent UI Header across all activities.
 * Updated: Now respects the custom Budget Cycle for the notification alert badge.
 */
object HeaderHelper {

    private var budgetListener: ListenerRegistration? = null
    private var expenseListener: ListenerRegistration? = null

    fun setupHeader(activity: Activity) {
        val sharedPreferences = activity.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        
        val userId = auth.currentUser?.uid ?: return

        // Update Session Activity timestamp
        sharedPreferences.edit().putLong("lastActive", System.currentTimeMillis()).apply()

        val profileIcon = activity.findViewById<View>(R.id.cv_profile_container)
        val notifContainer = activity.findViewById<View>(R.id.header_fl_notification)
        val badge = activity.findViewById<TextView>(R.id.header_notification_badge)

        profileIcon?.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            activity.startActivity(intent)
        }

        notifContainer?.setOnClickListener {
            val intent = Intent(activity, NotificationsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            activity.startActivity(intent)
        }

        activity.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            if (activity is ComponentActivity) activity.onBackPressedDispatcher.onBackPressed()
            else activity.onBackPressed()
        }

        val cycleDay = sharedPreferences.getInt("userBudgetCycleDay", 1)
        startNotificationMonitoring(userId, firestore, badge, cycleDay)
    }

    private fun startNotificationMonitoring(userId: String, db: FirebaseFirestore, badge: TextView?, cycleDay: Int) {
        if (badge == null) return
        val cycleRange = BudgetCycleHelper.getCurrentCycleRange(cycleDay)

        var budgets = listOf<DocumentSnapshot>()
        var expenses = listOf<DocumentSnapshot>()

        val updateBadgeUI = {
            var alertCount = 0
            budgets.forEach { bDoc ->
                val limit = (bDoc.get("maxGoal") as? Number)?.toDouble() 
                    ?: (bDoc.get("budgetAmount") as? Number)?.toDouble() ?: 0.0
                val catId = bDoc.getString("categoryId") ?: ""
                
                // Calculate spent for this category within the cycle
                val spent = expenses.filter { it.getString("categoryId") == catId && 
                    BudgetCycleHelper.isDateInRange(it.getString("date"), cycleRange) }
                    .sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
                
                if (limit > 0 && spent >= (limit * 0.8)) alertCount++
            }
            
            if (alertCount > 0) {
                badge.text = alertCount.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        }

        budgetListener?.remove()
        expenseListener?.remove()

        budgetListener = db.collection("category_budgets")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                budgets = snapshot?.documents ?: emptyList()
                updateBadgeUI()
            }

        expenseListener = db.collection("expense_entries")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                expenses = snapshot?.documents ?: emptyList()
                updateBadgeUI()
            }
    }
}
