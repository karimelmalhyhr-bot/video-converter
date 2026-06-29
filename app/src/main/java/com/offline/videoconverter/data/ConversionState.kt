package com.offline.videoconverter.data

data class ConversionState(
    val isProcessing: Boolean = false,
    val recordId: Long = -1L,
    val originalName: String = "",
    val targetFormat: String = "",
    val progress: Float = 0f, // From 0.0 to 1.0 (or 0% to 100%)
    val speed: String = "", // e.g. "1.2x"
    val timeElapsed: String = "", // e.g. "00:45"
    val error: String? = null
)
