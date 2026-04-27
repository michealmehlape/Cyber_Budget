package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Settings Activity: Allows users to view their profile, access security settings,
 * and manage general application preferences like currency.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        
        // --- SESSION VALIDATION ---
        if (!isSessionValid()) {
            logoutUser()
            return
        }

        setContentView(R.layout.activity_settings)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)
        
        userId = sharedPreferences.getInt("userId", -1)

        setupHeaderSection()
        setupMenu()
        setupAppSettings()
        setupNavbar()

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            logoutUser()
        }
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

    private fun setupHeaderSection() {
        val tvName = findViewById<TextView>(R.id.tv_user_name)
        val tvEmail = findViewById<TextView>(R.id.tv_user_email)
        val tvAlias = findViewById<TextView>(R.id.tv_user_alias)

        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            user?.let {
                tvName.text = "${it.firstName} ${it.lastName}"
                tvEmail.text = it.email
                tvAlias.text = "Alias: ${it.firstName}" 
            }
        }

        findViewById<Button>(R.id.btn_edit_profile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }

    private fun setupMenu() {
        // Profile
        val menuProfile = findViewById<View>(R.id.menu_profile)
        menuProfile.findViewById<ImageView>(R.id.menu_icon).setImageResource(R.drawable.profileicon)
        menuProfile.findViewById<TextView>(R.id.menu_text).text = "Profile"
        menuProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        // Notifications
        val menuNotifications = findViewById<View>(R.id.menu_notifications)
        menuNotifications.findViewById<ImageView>(R.id.menu_icon).setImageResource(R.drawable.notificationicon)
        menuNotifications.findViewById<TextView>(R.id.menu_text).text = "Notifications"
        menuNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Security
        val menuSecurity = findViewById<View>(R.id.menu_security)
        menuSecurity.findViewById<ImageView>(R.id.menu_icon).setImageResource(R.drawable.lockicon)
        menuSecurity.findViewById<TextView>(R.id.menu_text).text = "Security"
        menuSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }
    }

    private fun setupAppSettings() {
        // Currency
        val itemCurrency = findViewById<View>(R.id.item_currency)
        itemCurrency.findViewById<ImageView>(R.id.sub_icon).setImageResource(R.drawable.currencyicon)
        itemCurrency.findViewById<TextView>(R.id.sub_text).text = "Currency"
        itemCurrency.findViewById<TextView>(R.id.sub_value).text = "ZAR"

        // Account Settings
        val itemAccount = findViewById<View>(R.id.item_account)
        itemAccount.findViewById<ImageView>(R.id.sub_icon).setImageResource(R.drawable.settingsicon)
        itemAccount.findViewById<TextView>(R.id.sub_text).text = "Account Settings"

        // Dark Mode Logic Removed as it was causing glitches
    }

    private fun setupNavbar() {
        findViewById<ImageView>(R.id.nav_dash)?.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_analytics)?.setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_plus)?.setOnClickListener { startActivity(Intent(this, AddTransactionActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_card)?.setOnClickListener { startActivity(Intent(this, TransactionsActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_settings)?.setOnClickListener { /* Already here */ }
    }

    override fun onResume() {
        super.onResume()
        if (!isSessionValid()) {
            logoutUser()
        }
    }
}
