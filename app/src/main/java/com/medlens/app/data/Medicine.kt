package com.medlens.app.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey val genericName: String,
    val category: String,
    val brands: String,
    val uses: String,
    val mechanism: String,
    val sideEffects: String,
    val precautions: String,
    val contraindications: String,
    val interactions: String,
    val storage: String,
    val whenToSeeDoctor: String,
    val source: String,
    @ColumnInfo(defaultValue = "''") val howSupplied: String = "",
    @ColumnInfo(defaultValue = "''") val warnings: String = "",
    @ColumnInfo(defaultValue = "'Information unavailable'") val brandAvailability: String = "Information unavailable",
    @ColumnInfo(defaultValue = "''") val synonyms: String = "",
    @ColumnInfo(defaultValue = "'UNKNOWN'") val otcStatus: String = "UNKNOWN",
    @ColumnInfo(defaultValue = "''") val retrievedAt: String = "",
    @ColumnInfo(defaultValue = "''") val sourceRecordId: String = "",
    @ColumnInfo(defaultValue = "''") val sourceEffectiveDate: String = "",
    @ColumnInfo(defaultValue = "''") val sourceUrl: String = "",
    @ColumnInfo(defaultValue = "''") val productType: String = "",
    @ColumnInfo(defaultValue = "''") val unavailableFields: String = "",
)
