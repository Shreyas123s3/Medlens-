package com.medlens.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages", indices = [androidx.room.Index("sessionId"), androidx.room.Index(value = ["sessionId", "sentAt"])])
data class ChatMessage(
    @PrimaryKey val id: String,
    val sessionId: String,
    val accountId: String,
    val role: String,
    val text: String,
    val sentAt: Long = System.currentTimeMillis(),
)
