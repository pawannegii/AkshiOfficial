package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_reports")
data class InspectionReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val location: String = "Lasalgaon Mandi, Nashik",
    val inspectorName: String = "Pawan Negi (SIH-26031)",
    val totalOnions: Int,
    val gradeAPercentage: Double,
    val ursPercentage: Double,
    val damagedCount: Int,
    val rottenCount: Int,
    val sproutedCount: Int,
    val undersizedCount: Int,
    val notes: String,
    val imageUri: String? = null,
    val samplePresetType: String? = null, // e.g. "GRADE_A", "MIXED", "CUSTOM"
    val isFavorite: Boolean = false
)
