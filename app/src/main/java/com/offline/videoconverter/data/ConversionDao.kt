package com.offline.videoconverter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ConversionRecord>>

    @Query("SELECT * FROM conversion_records WHERE id = :id")
    suspend fun getRecordById(id: Long): ConversionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ConversionRecord): Long

    @Update
    suspend fun updateRecord(record: ConversionRecord)

    @Delete
    suspend fun deleteRecord(record: ConversionRecord)

    @Query("DELETE FROM conversion_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
