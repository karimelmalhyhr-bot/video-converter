package com.offline.videoconverter.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.offline.videoconverter.service.ConversionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ConversionManager {
    private val _conversionState = MutableStateFlow(ConversionState())
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

    fun updateProgress(
        isProcessing: Boolean,
        recordId: Long = -1L,
        originalName: String = "",
        targetFormat: String = "",
        progress: Float = 0f,
        speed: String = "",
        timeElapsed: String = "",
        error: String? = null
    ) {
        _conversionState.value = ConversionState(
            isProcessing = isProcessing,
            recordId = recordId,
            originalName = originalName,
            targetFormat = targetFormat,
            progress = progress,
            speed = speed,
            timeElapsed = timeElapsed,
            error = error
        )
    }

    fun startConversion(
        context: Context,
        inputUri: Uri,
        inputName: String,
        inputSize: Long,
        duration: Long,
        mode: String,
        targetFormat: String,
        qualityOrBitrate: String
    ) {
        val serviceIntent = Intent(context, ConversionService::class.java).apply {
            action = ConversionService.ACTION_START
            putExtra(ConversionService.EXTRA_INPUT_URI, inputUri.toString())
            putExtra(ConversionService.EXTRA_INPUT_NAME, inputName)
            putExtra(ConversionService.EXTRA_INPUT_SIZE, inputSize)
            putExtra(ConversionService.EXTRA_DURATION, duration)
            putExtra(ConversionService.EXTRA_MODE, mode)
            putExtra(ConversionService.EXTRA_TARGET_FORMAT, targetFormat)
            putExtra(ConversionService.EXTRA_QUALITY_BITRATE, qualityOrBitrate)
        }
        context.startForegroundService(serviceIntent)
    }

    fun cancelConversion(context: Context) {
        val serviceIntent = Intent(context, ConversionService::class.java).apply {
            action = ConversionService.ACTION_CANCEL
        }
        context.startService(serviceIntent)
    }
}
