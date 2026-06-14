package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

/**
 * NotificationsActivity: Displays budget alerts and system tips.
 * Updated: Respects custom Budget Cycle for real-time alerts.
 */
class NotificationsActivity : AppCompatActivity() {

    private val TAG = "Notifications_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    
    private var categoriesMap = mapOf<String, String>()
    private var budgetDocs = listOf<DocumentSnapshot>()
    private var expenseDocs = listOf<DocumentSnapshot>()
    
    private val listeners = mutableListOf<ListenerRegistration>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)
        HeaderHelper.setupHeader(this)

        val navContainer = findViewById<View>(R.id.include_nav)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            navContainer?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom
            }
            insets
        }

        rvNotifications = findViewById(R.id.rv_notifications)
        tvEmpty = findViewById(R.id.tv_empty)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        setupNavbar()
        startNotificationSync()
    }

    private fun startNotificationSync() {
        val userId = auth.currentUser?.uid ?: return
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val cycleDay = sharedPreferences.getInt("userBudgetCycleDay", 1)
        val cycleRange = BudgetCycleHelper.getCurrentCycleRange(cycleDay)

        // Independent listeners to avoid nesting leaks
        listeners.add(firestore.collection("categories").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                categoriesMap = snapshot?.documents?.associate { it.id to (it.getString("name") ?: "Unknown") } ?: emptyMap()
                generateAlerts()
            })

        listeners.add(firestore.collection("category_budgets").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                budgetDocs = snapshot?.documents ?: emptyList()
                generateAlerts()
            })

        listeners.add(firestore.collection("expense_entries").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val allExpenses = snapshot?.documents ?: emptyList()
                // Filter by Budget Cycle range instead of calendar month
                expenseDocs = allExpenses.filter { BudgetCycleHelper.isDateInRange(it.getString("date"), cycleRange) }
                generateAlerts()
            })
    }

    private fun generateAlerts() {
        val notificationList = mutableListOf<NotificationItem>()

        budgetDocs.forEach { bDoc ->
            val catId = bDoc.getString("categoryId") ?: ""
            val catName = categoriesMap[catId] ?: "Unknown"
            val limit = (bDoc.get("budgetAmount") as? Number)?.toDouble() ?: 0.0
            
            val spent = expenseDocs.filter { it.getString("categoryId") == catId }
                .sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
            
            if (limit > 0) {
                val percentage = (spent / limit) * 100
                if (percentage >= 100) {
                    val msg = "R${spent.toInt()} spent in $catName (Limit: R${limit.toInt()})"
                    notificationList.add(NotificationItem("Budget Exceeded", msg, true))
                    // Send system alert if not already in list to avoid spamming
                    NotificationHelper.showBudgetAlert(this, "Budget Exceeded", msg)
                } else if (percentage >= 80) {
                    val msg = "$catName is at ${percentage.toInt()}% of its budget cycle limit."
                    notificationList.add(NotificationItem("Budget Warning", msg, true))
                    NotificationHelper.showBudgetAlert(this, "Budget Warning", msg)
                }
            }
        }

        notificationList.add(NotificationItem("Pro Tip", "Keep up the daily logging to maintain your SAFE ZONE accuracy!", false))

        if (notificationList.size <= 1) {
            tvEmpty.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
            // Use distinct by title and message to avoid duplicate alert items in UI
            rvNotifications.adapter = NotificationAdapter(notificationList.distinctBy { it.message })
        }
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_profile)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java, false) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>, reorder: Boolean = true) {
        val intent = Intent(this, cls)
        if (reorder) intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }
}
