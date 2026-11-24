package com.medlens.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY genericName COLLATE NOCASE")
    fun all(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE genericName LIKE '%' || :q || '%' OR brands LIKE '%' || :q || '%' OR category LIKE '%' || :q || '%' ORDER BY genericName COLLATE NOCASE")
    fun search(q: String): List<Medicine>

    @Query("SELECT * FROM medicines WHERE genericName = :name LIMIT 1")
    fun get(name: String): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Medicine>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(medicine: Medicine)

    @Query("SELECT COUNT(*) FROM medicines")
    fun count(): Int
}
