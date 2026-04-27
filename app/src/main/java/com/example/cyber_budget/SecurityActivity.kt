package com.example.cyber_budget

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SecurityActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)
        
        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

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

            lifecycleScope.launch {
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    if (user.passwordHash == oldPass) {
                        val updatedUser = user.copy(passwordHash = newPass)
                        db.userDao().updateUser(updatedUser)
                        Toast.makeText(this@SecurityActivity, "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                        
                        // Clear fields
                        etOldPassword.text.clear()
                        etNewPassword.text.clear()
                        etConfirmNewPassword.text.clear()
                    } else {
                        Toast.makeText(this@SecurityActivity, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
