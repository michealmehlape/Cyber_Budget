package com.example.cyber_budget

/**
 * Data model for Income records. 
 * Maps directly to the 'income_entries' collection in Firestore.
 */
data class IncomeEntry(
    val id: Int = 0,
    val userId: String = "", 
    val source: String = "", 
    val amount: Double = 0.0,
    val date: String = ""
)
