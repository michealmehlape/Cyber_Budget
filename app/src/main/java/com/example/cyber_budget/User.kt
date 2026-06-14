package com.example.cyber_budget

/**
 * Model representing a User profile.
 * Updated to include persistent achievement badges.
 */
data class User(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val biometricEnabled: Boolean = false,
    val budgetCycleDay: Int = 1,
    val earnedBadges: List<String> = emptyList()
)
