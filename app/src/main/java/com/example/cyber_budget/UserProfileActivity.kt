package com.example.cyber_budget

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        db = AppDatabase.getDatabase(this)
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

        val etFirstName = findViewById<EditText>(R.id.et_first_name)
        val etLastName = findViewById<EditText>(R.id.et_last_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnBack = findViewById<ImageView>(R.id.btn_back)

        btnBack.setOnClickListener { finish() }

        // Load Current User Data
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            user?.let {
                etFirstName.setText(it.firstName)
                etLastName.setText(it.lastName)
                etEmail.setText(it.email)
            }
        }

        btnSave.setOnClickListener {
            val fName = etFirstName.text.toString().trim()
            val lName = etLastName.text.toString().trim()

            if (fName.isNotEmpty() && lName.isNotEmpty()) {
                lifecycleScope.launch {
                    val user = db.userDao().getUserById(userId)
                    user?.let {
                        val updatedUser = it.copy(firstName = fName, lastName = lName)
                        db.userDao().updateUser(updatedUser)
                        
                        // Update SharedPreferences
                        val editor = sharedPreferences.edit()
                        editor.putString("userFirstName", fName)
                        editor.apply()
                        
                        Toast.makeText(this@UserProfileActivity, "Profile Updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
