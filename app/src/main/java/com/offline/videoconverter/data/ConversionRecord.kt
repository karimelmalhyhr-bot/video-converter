package com.offline.videoconverter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_records")
data class ConversionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalName: String,
    val originalSize: Long,
    val targetFormat: String,
    val outputPath: String,
    val mimeType: String,
    val duration: Long, // in milliseconds
    val timestamp: Long, // when the job was added
    var status: String, // "PENDING", "PROCESSING", "SUCCESS", "FAILED", "CANCELLED"
    var errorMessage: String? = null,
    var outputSize: Long = 0L // final size in bytes
)
