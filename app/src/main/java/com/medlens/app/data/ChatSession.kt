package com.medlens.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String,
    val accountId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
)
