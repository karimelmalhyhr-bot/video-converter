package com.offline.videoconverter.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.offline.videoconverter.MainActivity
import com.offline.videoconverter.VideoConverterApp
import com.offline.videoconverter.data.AppDatabase
import com.offline.videoconverter.data.ConversionManager
import com.offline.videoconverter.data.ConversionRecord
import kotlinx.coroutines.*
import java.io.File
import java.util.Locale

class ConversionService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var activeSession: FFmpegSession? = null
    private var database: AppDatabase? = null
    private var isCancelled = false

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        database = AppDatabase.getInstance(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START -> {
                val inputUriStr = intent.getStringExtra(EXTRA_INPUT_URI) ?: ""
                val inputName = intent.getStringExtra(EXTRA_INPUT_NAME) ?: "input"
                val inputSize = intent.getLongExtra(EXTRA_INPUT_SIZE, 0L)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                val mode = intent.getStringExtra(EXTRA_MODE) ?: "VIDEO"
                val targetFormat = intent.getStringExtra(EXTRA_TARGET_FORMAT) ?: "mp4"
                val qualityBitrate = intent.getStringExtra(EXTRA_QUALITY_BITRATE) ?: "Medium"

                if (inputUriStr.isNotEmpty()) {
                    startForeground(NOTIFICATION_ID, createNotification("Preparing conversion...", 0f))
                    startConversionJob(
                        Uri.parse(inputUriStr),
                        inputName,
                        inputSize,
                        duration,
                        mode,
                        targetFormat.lowercase(Locale.ROOT),
                        qualityBitrate
                    )
                } else {
                    stopSelf()
                }
            }
            ACTION_CANCEL -> {
                cancelJob()
            }
        }

        return START_NOT_STICKY
    }

    private fun startConversionJob(
        inputUri: Uri,
        inputName: String,
        inputSize: Long,
        duration: Long,
        mode: String,
        targetFormat: String,
        qualityBitrate: String
    ) {
        serviceScope.launch {
            val recordId = withContext(Dispatchers.IO) {
                val record = ConversionRecord(
                    originalName = inputName,
                    originalSize = inputSize,
                    targetFormat = targetFormat,
                    outputPath = "",
                    mimeType = getMimeType(targetFormat),
                    duration = duration,
                    timestamp = System.currentTimeMillis(),
                    status = "PROCESSING"
                )
                database?.conversionDao()?.insertRecord(record) ?: -1L
            }

            if (recordId == -1L) {
                stopSelf()
                return@launch
            }

            ConversionManager.updateProgress(
                isProcessing = true,
                recordId = recordId,
                originalName = inputName,
                targetFormat = targetFormat,
                progress = 0f
            )

            val success = performConversion(
                inputUri,
                inputName,
                recordId,
                duration,
                mode,
                targetFormat,
                qualityBitrate
            )

            if (success) {
                showCompletionNotification("Conversion successful", "Finished converting $inputName")
            } else {
                if (isCancelled) {
                    showCompletionNotification("Conversion cancelled", "Task was aborted by user")
                } else {
                    showCompletionNotification("Conversion failed", "Error occurred during processing")
                }
            }

            ConversionManager.updateProgress(isProcessing = false)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private suspend fun performConversion(
        inputUri: Uri,
        inputName: String,
        recordId: Long,
        duration: Long,
        mode: String,
        targetFormat: String,
        qualityBitrate: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cacheDir = applicationContext.cacheDir
        val inputExt = getFileExtension(inputName) ?: "mp4"
        val tempInputFile = File(cacheDir, "input_temp_${System.currentTimeMillis()}.$inputExt")
        val tempOutputFile = File(cacheDir, "output_temp_${System.currentTimeMillis()}.$targetFormat")

        try {
            // Step 1: Copy selected Uri to local cache temp input
            copyUriToTempFile(applicationContext, inputUri, tempInputFile)

            // Step 2: Build FFmpeg command
            val ffmpegCommand = buildFFmpegCommand(
                tempInputFile.absolutePath,
                tempOutputFile.absolutePath,
                mode,
                targetFormat,
                qualityBitrate
            )

            // Step 3: Run FFmpeg async
            val startTime = System.currentTimeMillis()
            val deferred = CompletableDeferred<Boolean>()

            val session = FFmpegKit.executeAsync(
                ffmpegCommand,
                { completedSession ->
                    val returnCode = completedSession.returnCode
                    val state = completedSession.state
                    val isSuccess = returnCode.isValueSuccess
                    deferred.complete(isSuccess)
                },
                { /* log callback - can be added if verbose log is needed */ },
                { statistics ->
                    if (statistics != null) {
                        val timeInMillis = statistics.time
                        var progress = 0f
                        if (duration > 0) {
                            progress = (timeInMillis.toFloat() / duration).coerceIn(0f, 1f)
                        }
                        val speedVal = statistics.speed
                        val speedStr = String.format(Locale.US, "%.1fx", speedVal)
                        
                        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                        val elapsedStr = String.format(
                            Locale.US,
                            "%02d:%02d",
                            elapsedSeconds / 60,
                            elapsedSeconds % 60
                        )

                        // Update foreground notification
                        val pct = progress * 100
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            createNotification(
                                "Converting: ${String.format(Locale.US, "%.1f", pct)}% | Speed: $speedStr",
                                progress
                            )
                        )

                        // Update global manager state
                        ConversionManager.updateProgress(
                            isProcessing = true,
                            recordId = recordId,
                            originalName = inputName,
                            targetFormat = targetFormat,
                            progress = progress,
                            speed = speedStr,
                            timeElapsed = elapsedStr
                        )
                    }
                }
            )

            activeSession = session
            val conversionSuccess = deferred.await()

            if (conversionSuccess && !isCancelled) {
                // Step 4: Save output to public media storage
                val outputName = "Converted_${System.currentTimeMillis()}.$targetFormat"
                val mimeType = getMimeType(targetFormat)
                val savedUriStr = saveFileToPublicDirectory(
                    applicationContext,
                    tempOutputFile,
                    outputName,
                    mimeType,
                    mode
                )

                if (savedUriStr != null) {
                    val finalSize = tempOutputFile.length()
                    // Update database status to SUCCESS
                    val record = database?.conversionDao()?.getRecordById(recordId)
                    if (record != null) {
                        record.status = "SUCCESS"
                        record.outputPath = savedUriStr
                        record.outputSize = finalSize
                        database?.conversionDao()?.updateRecord(record)
                    }
                    return@withContext true
                }
            }

            // If we are here, either FFmpeg failed or save failed or it was cancelled
            val status = if (isCancelled) "CANCELLED" else "FAILED"
            val errorMsg = if (isCancelled) "Cancelled by user" else "Encoding/Storage error"
            
            val record = database?.conversionDao()?.getRecordById(recordId)
            if (record != null) {
                record.status = status
                record.errorMessage = errorMsg
                database?.conversionDao()?.updateRecord(record)
            }
            return@withContext false

        } catch (e: Exception) {
            val record = database?.conversionDao()?.getRecordById(recordId)
            if (record != null) {
                record.status = "FAILED"
                record.errorMessage = e.message ?: "Unknown service exception"
                database?.conversionDao()?.updateRecord(record)
            }
            return@withContext false
        } finally {
            // Clean up temp cache files
            if (tempInputFile.exists()) tempInputFile.delete()
            if (tempOutputFile.exists()) tempOutputFile.delete()
            activeSession = null
        }
    }

    private fun buildFFmpegCommand(
        inputPath: String,
        outputPath: String,
        mode: String,
        targetFormat: String,
        qualityBitrate: String
    ): String {
        return if (mode == "AUDIO") {
            // Audio extraction: -vn drops video stream, lame MP3 encoding
            val bitrate = when (qualityBitrate) {
                "64" -> "64k"
                "128" -> "128k"
                "192" -> "192k"
                "320" -> "320k"
                else -> "192k"
            }
            "-y -i \"$inputPath\" -vn -c:a libmp3lame -b:a $bitrate \"$outputPath\""
        } else {
            // Video format conversion
            when (targetFormat) {
                "webm" -> {
                    // VP8 encoding (fast preset speed 4, audio vorbis)
                    val crf = when (qualityBitrate) {
                        "Low" -> "36"
                        "Medium" -> "28"
                        "High" -> "20"
                        "Original" -> "15"
                        else -> "28"
                    }
                    "-y -i \"$inputPath\" -c:v libvpx -crf $crf -b:v 1M -speed 4 -c:a libvorbis -b:a 128k \"$outputPath\""
                }
                else -> {
                    // MP4, MKV, MOV (H.264 ultrafast, audio aac)
                    val crf = when (qualityBitrate) {
                        "Low" -> "28"
                        "Medium" -> "23"
                        "High" -> "18"
                        "Original" -> "15"
                        else -> "23"
                    }
                    "-y -i \"$inputPath\" -c:v libx264 -preset ultrafast -crf $crf -c:a aac -b:a 128k \"$outputPath\""
                }
            }
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, tempFile: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private suspend fun saveFileToPublicDirectory(
        context: Context,
        tempFile: File,
        fileName: String,
        mimeType: String,
        mode: String
    ): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = if (mode == "VIDEO") {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val folderName = if (mode == "VIDEO") Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
            val relativePath = "$folderName/VideoConverter"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                if (mode == "VIDEO") {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val fileUri = resolver.insert(collection, values) ?: return@withContext null

            try {
                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                values.clear()
                if (mode == "VIDEO") {
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                } else {
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(fileUri, values, null, null)
                return@withContext fileUri.toString()
            } catch (e: Exception) {
                resolver.delete(fileUri, null, null)
                return@withContext null
            }
        } else {
            val folderName = if (mode == "VIDEO") Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
            val publicDir = Environment.getExternalStoragePublicDirectory(folderName)
            val customDir = File(publicDir, "VideoConverter")
            if (!customDir.exists()) {
                customDir.mkdirs()
            }
            val destinationFile = File(customDir, fileName)
            try {
                tempFile.inputStream().use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                var scannedUriStr: String? = null
                val lock = CompletableDeferred<Unit>()
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destinationFile.absolutePath),
                    arrayOf(mimeType)
                ) { _, uri ->
                    scannedUriStr = uri?.toString() ?: Uri.fromFile(destinationFile).toString()
                    lock.complete(Unit)
                }
                lock.await()
                return@withContext scannedUriStr
            } catch (e: Exception) {
                return@withContext null
            }
        }
    }

    private fun cancelJob() {
        isCancelled = true
        activeSession?.let { session ->
            FFmpegKit.cancel(session.sessionId)
        }
    }

    private fun createNotification(content: String, progress: Float): Notification {
        val cancelIntent = Intent(this, ConversionService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, VideoConverterApp.CHANNEL_ID)
            .setContentTitle("Media Transcoder")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, (progress * 100).toInt(), false)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )
            .build()
    }

    private fun showCompletionNotification(title: String, text: String) {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, VideoConverterApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun getFileExtension(fileName: String): String? {
        val lastIndex = fileName.lastIndexOf('.')
        return if (lastIndex != -1 && lastIndex < fileName.length - 1) {
            fileName.substring(lastIndex + 1)
        } else {
            null
        }
    }

    private fun getMimeType(format: String): String {
        return when (format.lowercase(Locale.ROOT)) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            else -> "video/*"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.offline.videoconverter.action.START"
        const val ACTION_CANCEL = "com.offline.videoconverter.action.CANCEL"

        const val EXTRA_INPUT_URI = "extra_input_uri"
        const val EXTRA_INPUT_NAME = "extra_input_name"
        const val EXTRA_INPUT_SIZE = "extra_input_size"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_TARGET_FORMAT = "extra_target_format"
        const val EXTRA_QUALITY_BITRATE = "extra_quality_bitrate"
    }
}
