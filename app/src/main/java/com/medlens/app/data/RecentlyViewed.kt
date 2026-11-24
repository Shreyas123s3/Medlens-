package com.medlens.app.data

import androidx.room.Entity

@Entity(tableName = "recently_viewed", primaryKeys = ["accountId", "genericName"])
data class RecentlyViewed(val accountId: String, val genericName: String, val viewedAt: Long = System.currentTimeMillis())
