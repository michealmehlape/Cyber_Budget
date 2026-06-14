package com.example.cyber_budget

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

/**
 * Activity for managing user-defined spending categories.
 * Features real-time sync, editing, and deletion of categories.
 * Updated to assign unique bright colors to each category and clean up orphan budgets.
 */
class AddCategoryActivity : AppCompatActivity() {

    private val TAG = "AddCategory_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvCategories: RecyclerView

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
        setContentView(R.layout.activity_add_category)
        HeaderHelper.setupHeader(this)

        // Standardized Navbar position
        val navContainer = findViewById<View>(R.id.include_nav)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            navContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom
            }
            insets
        }

        val etCategoryName = findViewById<EditText>(R.id.et_category_name)
        val btnSave = findViewById<Button>(R.id.btn_save_category)
        rvCategories = findViewById(R.id.rv_categories)
        rvCategories.layoutManager = LinearLayoutManager(this)

        btnSave.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            val userId = auth.currentUser?.uid
            
            if (name.isNotEmpty() && userId != null) {
                val color = generateBrightColor()
                val categoryMap = hashMapOf(
                    "name" to name,
                    "userId" to userId,
                    "color" to color
                )
                firestore.collection("categories").add(categoryMap)
                    .addOnSuccessListener {
                        etCategoryName.text.clear()
                        Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }

        setupNavbar()
        startRealTimeCategories()
    }

    private fun generateBrightColor(): String {
        val h = Random.nextFloat() * 360
        val s = 0.7f + Random.nextFloat() * 0.3f // High saturation for brightness
        val v = 0.8f + Random.nextFloat() * 0.2f // High value for brightness
        val color = Color.HSVToColor(floatArrayOf(h, s, v))
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun startRealTimeCategories() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("categories")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                val categories = documents?.map { doc ->
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        userId = doc.getString("userId") ?: "",
                        color = doc.getString("color") ?: "#000000"
                    )
                } ?: emptyList()
                
                rvCategories.adapter = CategoryAdapter(
                    categories,
                    onEditClick = { category -> showEditCategoryDialog(category) },
                    onDeleteClick = { category -> showDeleteCategoryConfirmation(category) }
                )
            }
    }

    private fun showEditCategoryDialog(category: Category) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Category")

        val input = EditText(this)
        input.setText(category.name)
        builder.setView(input)

        builder.setPositiveButton("Update") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                firestore.collection("categories").document(category.id)
                    .update("name", newName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showDeleteCategoryConfirmation(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This will also remove any budget goals set for this category.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategoryAndBudgets(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategoryAndBudgets(category: Category) {
        val userId = auth.currentUser?.uid ?: return

        // 1. Delete the category itself
        firestore.collection("categories").document(category.id).delete()
            .addOnSuccessListener {
                // 2. Find and delete any budgets linked to this category ID
                firestore.collection("category_budgets")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("categoryId", category.id)
                    .get()
                    .addOnSuccessListener { budgetDocs ->
                        val batch = firestore.batch()
                        for (doc in budgetDocs) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().addOnSuccessListener {
                            Toast.makeText(this, "Category and associated budgets deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
        findViewById<View>(R.id.ll_nav_profile)?.setOnClickListener { navigateTo(SettingsActivity::class.java) }
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>) {
        val intent = Intent(this, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
}
