package com.medlens.app.data

import androidx.room.Entity

@Entity(tableName = "recent_searches", primaryKeys = ["accountId", "query"])
data class RecentSearch(val accountId: String, val query: String, val searchedAt: Long = System.currentTimeMillis())
