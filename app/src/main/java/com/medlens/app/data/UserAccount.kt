package com.medlens.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts", indices = [androidx.room.Index(value = ["email"], unique = true)])
data class UserAccount(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordSalt: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
)
