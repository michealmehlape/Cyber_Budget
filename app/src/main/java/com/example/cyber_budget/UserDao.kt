package com.example.cyber_budget

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    // Update user details
    @Update
    suspend fun updateUser(user: User)

    // Allow logging in with either email or username (matching both requirement text and UI)
    @Query("SELECT * FROM users WHERE email = :identifier AND passwordHash = :password LIMIT 1")
    suspend fun login(identifier: String, password: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?
}
