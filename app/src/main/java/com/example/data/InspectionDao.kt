package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspection_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<InspectionReportEntity>>

    @Query("SELECT * FROM inspection_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Long): InspectionReportEntity?

    @Query("SELECT * FROM inspection_reports WHERE batchId = :batchId LIMIT 1")
    suspend fun getReportByBatchId(batchId: String): InspectionReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: InspectionReportEntity): Long

    @Delete
    suspend fun deleteReport(report: InspectionReportEntity)

    @Query("DELETE FROM inspection_reports")
    suspend fun clearAll()
}
