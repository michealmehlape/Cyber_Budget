package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Settings Activity: Orchestrates user profile navigation and app preferences.
 * Updated: Standardized Navbar positioning and Budget Cycle support.
 */
class SettingsActivity : AppCompatActivity() {

    private val TAG = "Settings_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private var profileListener: ListenerRegistration? = null

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
        setContentView(R.layout.activity_settings)
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
        
        setupMenu()
        setupAppSettings()
        setupNavbar()
        startProfileSync()

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            Log.i(TAG, "User logging out")
            logoutUser()
        }
    }

    private fun startProfileSync() {
        val userId = auth.currentUser?.uid ?: return
        val tvName = findViewById<TextView>(R.id.tv_user_name)
        val tvEmail = findViewById<TextView>(R.id.tv_user_email)
        val itemCycle = findViewById<View>(R.id.item_payday)
        val tvCycleValue = itemCycle.findViewById<TextView>(R.id.sub_value)

        profileListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val fName = snapshot.getString("firstName") ?: "User"
                    val lName = snapshot.getString("lastName") ?: ""
                    tvName.text = "$fName $lName"
                    tvEmail.text = snapshot.getString("email") ?: "No Email Found"
                    
                    val cycleDay = snapshot.getLong("budgetCycleDay")?.toInt() 
                        ?: snapshot.getLong("payDay")?.toInt() // Backward compatibility
                        ?: 1
                    
                    tvCycleValue.text = "Day $cycleDay"
                    
                    sharedPreferences.edit().apply {
                        putString("userFirstName", fName)
                        putInt("userBudgetCycleDay", cycleDay)
                        apply()
                    }
                }
            }
    }

    private fun logoutUser() {
        auth.signOut()
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupMenu() {
        // Profile Management
        val menuProfile = findViewById<View>(R.id.menu_profile)
        menuProfile.findViewById<ImageView>(R.id.menu_icon).setImageResource(R.drawable.profileicon)
        menuProfile.findViewById<TextView>(R.id.menu_text).text = "Profile Management"
        menuProfile.setOnClickListener { navigateTo(UserProfileActivity::class.java) }

        // Notifications
        val menuNotifications = findViewById<View>(R.id.menu_notifications)
        menuNotifications.findViewById<ImageView>(R.id.menu_icon).setImageResource(R.drawable.notificationicon)
        menuNotifications.findViewById<TextView>(R.id.menu_text).text = "Notifications"
        menuNotifications.setOnClickListener { navigateTo(NotificationsActivity::class.java) }

        // Account Security
        val menuSecurity = findViewById<View>(R.id.menu_security)
        menuSecurity.findViewById<ImageView>(R.id.menu_icon).setImageResource(R.drawable.lockicon)
        menuSecurity.findViewById<TextView>(R.id.menu_text).text = "Account Security"
        menuSecurity.setOnClickListener { navigateTo(SecurityActivity::class.java) }

        // Achievement Badges
        val menuBadges = findViewById<View>(R.id.menu_badges)
        menuBadges.findViewById<ImageView>(R.id.menu_icon).setImageResource(android.R.drawable.btn_star_big_on)
        menuBadges.findViewById<TextView>(R.id.menu_text).text = "My Achievement Badges"
        menuBadges.setOnClickListener { navigateTo(BadgesActivity::class.java) }
    }

    private fun setupAppSettings() {
        val itemCurrency = findViewById<View>(R.id.item_currency)
        itemCurrency.findViewById<ImageView>(R.id.sub_icon).setImageResource(R.drawable.walleticon)
        itemCurrency.findViewById<TextView>(R.id.sub_text).text = "Default Currency"
        itemCurrency.findViewById<TextView>(R.id.sub_value).text = "ZAR"

        val itemCycle = findViewById<View>(R.id.item_payday)
        itemCycle.findViewById<ImageView>(R.id.sub_icon).setImageResource(R.drawable.dashicon)
        itemCycle.findViewById<TextView>(R.id.sub_text).text = "Budget Cycle"
        itemCycle.setOnClickListener { showBudgetCyclePickerDialog() }

        val itemAccount = findViewById<View>(R.id.item_account)
        itemAccount.findViewById<ImageView>(R.id.sub_icon).setImageResource(R.drawable.settingsicon)
        itemAccount.findViewById<TextView>(R.id.sub_text).text = "Account Status"
        itemAccount.findViewById<TextView>(R.id.sub_value).text = "Standard"
    }

    private fun showBudgetCyclePickerDialog() {
        val userId = auth.currentUser?.uid ?: return
        val currentDay = sharedPreferences.getInt("userBudgetCycleDay", 1)
        
        val picker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 31
            value = currentDay
        }

        AlertDialog.Builder(this)
            .setTitle("Select Budget Cycle Start")
            .setMessage("Choose the day your monthly budget cycle starts.")
            .setView(picker)
            .setPositiveButton("Save") { _, _ ->
                val newDay = picker.value
                firestore.collection("users").document(userId)
                    .update("budgetCycleDay", newDay)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Budget Cycle updated to Day $newDay", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_profile)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java, false) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>, useReorder: Boolean = true) {
        val intent = Intent(this, cls)
        if (useReorder) intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        profileListener?.remove()
    }
}
