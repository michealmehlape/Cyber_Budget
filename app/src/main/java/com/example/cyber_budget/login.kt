package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Login Activity: Handles user authentication using the Room Database and Biometrics.
 */
class login : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        
        // --- CHECK IF USER IS ALREADY LOGGED IN AND SESSION IS VALID ---
        if (isSessionValid()) {
            navigateToMain()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        db = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvClickHere = findViewById<TextView>(R.id.tv_click_here)
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val ivBiometricTrigger = findViewById<ImageView>(R.id.iv_biometric_trigger)

        tvClickHere?.setOnClickListener {
            startActivity(Intent(this, register::class.java))
        }

        btnBack?.setOnClickListener {
            finish()
        }

        btnLogin?.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = db.userDao().login(email, password)
                if (user != null) {
                    saveLoginState(user.id, user.firstName)
                    Toast.makeText(this@login, "Login Successful! Welcome ${user.firstName}", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this@login, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- BIOMETRIC SETUP ---
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        
        val isBiometricEnabled = sharedPreferences.getBoolean("biometricEnabled", false)

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS && isBiometricEnabled) {
            ivBiometricTrigger.visibility = View.VISIBLE
            ivBiometricTrigger.setOnClickListener {
                showBiometricPrompt()
            }
            // Auto-trigger on open if biometric is enabled
            showBiometricPrompt()
        } else {
            ivBiometricTrigger.visibility = View.GONE
        }
    }

    private fun isSessionValid(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (!isLoggedIn) return false
        
        val lastActive = sharedPreferences.getLong("lastActive", 0L)
        val currentTime = System.currentTimeMillis()
        val sessionDuration = 30 * 60 * 1000 // 30 minutes session expiry
        
        return (currentTime - lastActive) < sessionDuration
    }

    private fun saveLoginState(userId: Int, firstName: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putInt("userId", userId)
        editor.putString("userFirstName", firstName)
        editor.putLong("lastActive", System.currentTimeMillis())
        editor.apply()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // SUCCESS: Reactivate session and proceed
                sharedPreferences.edit()
                    .putBoolean("isLoggedIn", true)
                    .putLong("lastActive", System.currentTimeMillis())
                    .apply()
                navigateToMain()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(applicationContext, errString, Toast.LENGTH_SHORT).show()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
