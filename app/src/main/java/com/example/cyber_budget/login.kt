package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Login Activity: handles authentication and biometric session management.
 * Fixed: Robust biometric re-entry logic and error handling.
 */
class login : AppCompatActivity() {

    private val TAG = "Auth_Login"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private var isBiometricInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Auto-login if session is active
        if (isSessionValid() && auth.currentUser != null) {
            navigateToMain()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtons()
        checkBiometricAvailability()
    }

    private fun setupButtons() {
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvRegister = findViewById<TextView>(R.id.tv_click_here)

        tvRegister?.setOnClickListener { 
            startActivity(Intent(this, register::class.java)) 
        }

        btnLogin?.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        fetchProfileAndNavigate(auth.currentUser?.uid ?: "")
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun checkBiometricAvailability() {
        val ivBiometric = findViewById<ImageView>(R.id.iv_biometric_trigger)
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val isBioEnabled = sharedPreferences.getBoolean("biometricEnabled", false)

        // Show biometric trigger if enabled and Firebase has a cached user (session lock scenario)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS && isBioEnabled && auth.currentUser != null) {
            ivBiometric.visibility = View.VISIBLE
            ivBiometric.setOnClickListener { showBiometricPrompt() }
            // Auto-trigger prompt for convenience
            ivBiometric.postDelayed({ showBiometricPrompt() }, 300)
        } else {
            ivBiometric.visibility = View.GONE
        }
    }

    private fun fetchProfileAndNavigate(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val fName = doc.getString("firstName") ?: "User"
                saveLoginState(userId, fName)
                navigateToMain()
            }
            .addOnFailureListener {
                saveLoginState(userId, "User")
                navigateToMain()
            }
    }

    private fun isSessionValid(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val lastActive = sharedPreferences.getLong("lastActive", 0L)
        // 30 minute session validity
        return isLoggedIn && (System.currentTimeMillis() - lastActive) < (30 * 60 * 1000)
    }

    private fun saveLoginState(userId: String, firstName: String) {
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("userId", userId)
            putString("userFirstName", firstName)
            putLong("lastActive", System.currentTimeMillis())
            apply()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showBiometricPrompt() {
        if (isBiometricInProgress) return
        isBiometricInProgress = true

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricInProgress = false
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    fetchProfileAndNavigate(userId)
                } else {
                    Toast.makeText(this@login, "Session expired. Please log in with password.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricInProgress = false
                Log.e(TAG, "Biometric error: $errString ($errorCode)")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                isBiometricInProgress = false
                Toast.makeText(this@login, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Quick Unlock")
            .setSubtitle("Authenticate to access your budget")
            .setNegativeButtonText("Use Password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        try {
            prompt.authenticate(info)
        } catch (e: Exception) {
            isBiometricInProgress = false
            Log.e(TAG, "Prompt failed to launch", e)
        }
    }
}
