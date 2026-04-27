package com.example.cyber_budget

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Register Activity: Handles new user registration and saves data to the Room Database.
 */
class register : AppCompatActivity() {

    // Database instance
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Get references to the UI elements
        val etFirstName = findViewById<EditText>(R.id.et_first_name)
        val etLastName = findViewById<EditText>(R.id.et_last_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvLoginClick = findViewById<TextView>(R.id.tv_login_click)
        val btnBack = findViewById<android.widget.ImageView>(R.id.btn_back)

        // 2. Navigation: Back button
        btnBack.setOnClickListener {
            finish()
        }

        // 3. Navigation: Go back to Login page
        tvLoginClick.setOnClickListener {
            finish() // Since we usually come from the login page, finish() takes us back
        }

        // 4. Handle Register Button Click
        btnRegister.setOnClickListener {
            val fName = etFirstName.text.toString().trim()
            val lName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            // Basic Validation
            if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Database Operation: Save new user
            lifecycleScope.launch {
                // Check if email already exists
                val existingUser = db.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@register, "Email already registered", Toast.LENGTH_SHORT).show()
                } else {
                    // Create new user object
                    val newUser = User(
                        firstName = fName,
                        lastName = lName,
                        email = email,
                        passwordHash = pass // Note: In a real app, hash this!
                    )
                    
                    // Insert into database
                    db.userDao().insertUser(newUser)
                    
                    Toast.makeText(this@register, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    
                    // Go to Login page
                    finish()
                }
            }
        }
    }
}
