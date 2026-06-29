package com.offline.videoconverter

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offline.videoconverter.data.AppDatabase
import com.offline.videoconverter.data.ConversionManager
import com.offline.videoconverter.data.ConversionRecord
import com.offline.videoconverter.data.ConversionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val contentResolver = application.contentResolver

    // Navigation and workflow states
    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _selectedVideoName = MutableStateFlow("")
    val selectedVideoName: StateFlow<String> = _selectedVideoName.asStateFlow()

    private val _selectedVideoSize = MutableStateFlow(0L)
    val selectedVideoSize: StateFlow<Long> = _selectedVideoSize.asStateFlow()

    private val _selectedVideoDuration = MutableStateFlow(0L)
    val selectedVideoDuration: StateFlow<Long> = _selectedVideoDuration.asStateFlow()

    // Live conversion state flow (from Singleton Manager)
    val conversionState: StateFlow<ConversionState> = ConversionManager.conversionState

    // Reactive database history list
    val historyRecords: StateFlow<List<ConversionRecord>> = database.conversionDao()
        .getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectVideo(uri: Uri) {
        viewModelScope.launch {
            var name = "unknown_video"
            var size = 0L
            var duration = 0L

            withContext(Dispatchers.IO) {
                // Get Display Name and Size
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) name = cursor.getString(nameIndex)
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }

                // Get Duration using MediaMetadataRetriever
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(getApplication(), uri)
                    val durationStr = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                    duration = durationStr?.toLongOrNull() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _selectedVideoUri.value = uri
            _selectedVideoName.value = name
            _selectedVideoSize.value = size
            _selectedVideoDuration.value = duration
        }
    }

    fun clearSelection() {
        _selectedVideoUri.value = null
        _selectedVideoName.value = ""
        _selectedVideoSize.value = 0L
        _selectedVideoDuration.value = 0L
    }

    fun startConversion(
        mode: String,
        targetFormat: String,
        qualityOrBitrate: String
    ) {
        val uri = _selectedVideoUri.value ?: return
        val name = _selectedVideoName.value
        val size = _selectedVideoSize.value
        val duration = _selectedVideoDuration.value

        ConversionManager.startConversion(
            context = getApplication(),
            inputUri = uri,
            inputName = name,
            inputSize = size,
            duration = duration,
            mode = mode,
            targetFormat = targetFormat,
            qualityOrBitrate = qualityOrBitrate
        )
    }

    fun cancelActiveConversion() {
        ConversionManager.cancelConversion(getApplication())
    }

    fun openFile(context: Context, record: ConversionRecord) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(record.outputPath), record.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Handle case where no app can open this specific file format
            }
        }
    }

    fun shareFile(context: Context, record: ConversionRecord) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = record.mimeType
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(record.outputPath))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share File"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun renameFile(context: Context, record: ConversionRecord, newNameWithoutExt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(record.outputPath)
                val currentName = record.originalName
                val ext = getFileExtension(currentName) ?: record.targetFormat
                val finalNewName = "$newNameWithoutExt.$ext"

                if (uri.scheme == "content") {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, finalNewName)
                    }
                    contentResolver.update(uri, values, null, null)
                } else {
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        val parent = file.parentFile
                        val newFile = File(parent, finalNewName)
                        if (file.renameTo(newFile)) {
                            // File renamed locally
                        }
                    }
                }

                // Update db
                val updatedRecord = record.copy(
                    originalName = finalNewName
                )
                database.conversionDao().updateRecord(updatedRecord)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRecord(record: ConversionRecord, deleteFromStorage: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (deleteFromStorage) {
                    val uri = Uri.parse(record.outputPath)
                    if (uri.scheme == "content") {
                        contentResolver.delete(uri, null, null)
                    } else {
                        val file = File(uri.path ?: "")
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                database.conversionDao().deleteRecord(record)
            }
        }
    }

    private fun getFileExtension(fileName: String): String? {
        val lastIndex = fileName.lastIndexOf('.')
        return if (lastIndex != -1 && lastIndex < fileName.length - 1) {
            fileName.substring(lastIndex + 1)
        } else {
            null
        }
    }
}
