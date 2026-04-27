package com.example.cyber_budget

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        db = AppDatabase.getDatabase(this)
        HeaderHelper.setupHeader(this, db)

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)

        val etAmount = findViewById<EditText>(R.id.et_amount)
        val etDescription = findViewById<EditText>(R.id.et_description)
        val etDate = findViewById<EditText>(R.id.et_date)
        val etStart = findViewById<EditText>(R.id.et_start_time)
        val etEnd = findViewById<EditText>(R.id.et_end_time)
        val spinner = findViewById<Spinner>(R.id.spinner_category)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnAddNewCategory = findViewById<TextView>(R.id.btn_add_category)
        val rgType = findViewById<RadioGroup>(R.id.rg_type)

        // 1. Setup Date & Time Pickers
        setupPickers(etDate, etStart, etEnd)

        // 2. Link to Add Category Page
        btnAddNewCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        // 3. Load Categories from DB
        loadCategories(spinner)

        // 4. Save Transaction Logic
        btnSave.setOnClickListener {
            saveTransaction(etAmount, etDescription, etDate, etStart, etEnd, spinner, rgType)
        }

        setupNavbar()
    }

    private fun setupPickers(etDate: EditText, etStart: EditText, etEnd: EditText) {
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val timeSetListener = { target: EditText ->
            TimePickerDialog(this, { _, h, min ->
                target.setText(String.format(Locale.US, "%02d:%02d", h, min))
            }, 12, 0, true).show()
        }
        
        etStart.setOnClickListener { timeSetListener(etStart) }
        etEnd.setOnClickListener { timeSetListener(etEnd) }
    }

    private fun loadCategories(spinner: Spinner) {
        lifecycleScope.launch {
            val categories = db.categoryDao().getCategoriesForUser(userId)
            val names = categories.map { it.name }
            val adapter = ArrayAdapter(this@AddTransactionActivity, android.R.layout.simple_spinner_dropdown_item, names)
            spinner.adapter = adapter
        }
    }

    private fun saveTransaction(etAmount: EditText, etDesc: EditText, etDate: EditText, etStart: EditText, etEnd: EditText, spinner: Spinner, rgType: RadioGroup) {
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val desc = etDesc.text.toString()
        val date = etDate.text.toString()
        val start = etStart.text.toString()
        val end = etEnd.text.toString()
        val categoryName = spinner.selectedItem?.toString() ?: ""
        
        // Determine if it's Income or Expense
        val isIncome = rgType.checkedRadioButtonId == R.id.rb_income

        if (amount > 0) {
            lifecycleScope.launch {
                if (isIncome) {
                    // SAVE AS INCOME
                    val income = IncomeEntry(
                        userId = userId,
                        source = categoryName, // Use category as source for income
                        amount = amount,
                        date = date
                    )
                    db.incomeEntryDao().insertIncome(income)
                    Toast.makeText(this@AddTransactionActivity, "Income Saved", Toast.LENGTH_SHORT).show()
                } else {
                    // SAVE AS EXPENSE
                    val category = db.categoryDao().getCategoriesForUser(userId).find { it.name == categoryName }
                    val expense = ExpenseEntry(
                        userId = userId,
                        categoryId = category?.id ?: 0,
                        date = date,
                        startTime = start,
                        endTime = end,
                        description = desc,
                        amount = amount
                    )
                    db.expenseEntryDao().insertExpense(expense)
                    Toast.makeText(this@AddTransactionActivity, "Expense Saved", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        } else {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavbar() {
        findViewById<ImageView>(R.id.nav_dash).setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_analytics).setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_card).setOnClickListener { startActivity(Intent(this, TransactionsActivity::class.java)) }
        findViewById<ImageView>(R.id.nav_settings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
}
