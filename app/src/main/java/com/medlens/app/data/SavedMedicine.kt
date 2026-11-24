package com.medlens.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_medicines", primaryKeys = ["accountId", "genericName"])
data class SavedMedicine(val accountId: String, val genericName: String, val savedAt: Long = System.currentTimeMillis())
