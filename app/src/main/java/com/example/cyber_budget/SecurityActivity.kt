package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.firebase.auth.FirebaseAuth

/**
 * Security Activity: Manages sensitive user settings including password updates 
 * and biometric authentication preferences.
 */
class SecurityActivity : AppCompatActivity() {

    private val TAG = "Security_Activity"
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_security)
        HeaderHelper.setupHeader(this)

        // Fix Navbar position and handle screen edges
        val navContainer = findViewById<View>(R.id.include_nav)
        val mainRoot = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            navContainer?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // Sits consistently above the system navigation bar
                bottomMargin = navBars.bottom + 24
            }
            insets
        }

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val switchBiometric = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_biometric)
        val etOldPassword = findViewById<EditText>(R.id.et_old_password)
        val etNewPassword = findViewById<EditText>(R.id.et_new_password)
        val etConfirmNewPassword = findViewById<EditText>(R.id.et_confirm_new_password)
        val btnUpdate = findViewById<Button>(R.id.btn_update_password)

        // --- SETUP BIOMETRIC TOGGLE ---
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            switchBiometric.isEnabled = false
            switchBiometric.alpha = 0.5f 
        } else {
            switchBiometric.isChecked = sharedPreferences.getBoolean("biometricEnabled", false)
            switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean("biometricEnabled", isChecked).apply()
                val status = if (isChecked) "enabled" else "disabled"
                Toast.makeText(this, "Biometric login $status", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpdate.setOnClickListener {
            val oldPass = etOldPassword.text.toString()
            val newPass = etNewPassword.text.toString()
            val confirmPass = etConfirmNewPassword.text.toString()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null) {
                user.updatePassword(newPass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                            etOldPassword.text.clear()
                            etNewPassword.text.clear()
                            etConfirmNewPassword.text.clear()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        setupNavbar()
    }

    private fun setupNavbar() {
        // Highlight profile tab
        findViewById<View>(R.id.ll_nav_profile)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>) {
        val intent = Intent(this, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
}
