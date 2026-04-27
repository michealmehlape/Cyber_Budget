package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private lateinit var rvCategories: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        db = AppDatabase.getDatabase(this)
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

        val etCategoryName = findViewById<EditText>(R.id.et_category_name)
        val btnSave = findViewById<Button>(R.id.btn_save_category)
        rvCategories = findViewById<RecyclerView>(R.id.rv_categories)
        rvCategories.layoutManager = LinearLayoutManager(this)

        btnSave.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    db.categoryDao().insertCategory(Category(name = name, userId = userId))
                    etCategoryName.text.clear()
                    loadCategories()
                    Toast.makeText(this@AddCategoryActivity, "Category added", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        
        setupNavbar()
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getCategoriesForUser(userId)
            val adapter = CategoryAdapter(categories)
            rvCategories.adapter = adapter
        }
    }

    private fun setupNavbar() {
        findViewById<ImageView>(R.id.nav_dash).setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_analytics).setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_plus).setOnClickListener { startActivity(Intent(this, AddTransactionActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_card).setOnClickListener { startActivity(Intent(this, TransactionsActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_settings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
}
