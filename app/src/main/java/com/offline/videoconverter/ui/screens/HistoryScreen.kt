package com.offline.videoconverter.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.videoconverter.MainViewModel
import com.offline.videoconverter.data.ConversionRecord
import com.offline.videoconverter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeState by viewModel.conversionState.collectAsState()
    val records by viewModel.historyRecords.collectAsState()

    // Dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var recordToRename by remember { mutableStateOf<ConversionRecord?>(null) }
    var renameText by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ConversionRecord?>(null) }
    var deletePhysicalFile by remember { mutableStateOf(true) }

    var showInfoDialog by remember { mutableStateOf(false) }
    var recordForInfo by remember { mutableStateOf<ConversionRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History & Queue",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Go back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        containerColor = DarkBg,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Glowing background element
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(SecondaryViolet.copy(alpha = 0.06f), Color.Transparent)
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Job Section
                if (activeState.isProcessing) {
                    item {
                        Text(
                            text = "ACTIVE CONVERSION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, PrimaryPurple.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .shadow(12.dp, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeState.originalName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Transcoding to ${activeState.targetFormat.uppercase()}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.cancelActiveConversion() },
                                        modifier = Modifier
                                            .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Cancel Conversion",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Linear Progress
                                LinearProgressIndicator(
                                    progress = activeState.progress,
                                    color = PrimaryPurple,
                                    trackColor = BorderColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = "Speed",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activeState.speed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Text(
                                        text = "${(activeState.progress * 100).toInt()}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AccentCyan
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Timer,
                                            contentDescription = "Time",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activeState.timeElapsed,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Header for Finished items
                item {
                    Text(
                        text = "CONVERSION HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (records.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = "Empty",
                                    tint = BorderColor,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No conversion history",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // History List Items
                items(records, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        onPlay = { viewModel.openFile(context, record) },
                        onShare = { viewModel.shareFile(context, record) },
                        onRename = {
                            recordToRename = record
                            renameText = record.originalName.substringBeforeLast('.')
                            showRenameDialog = true
                        },
                        onDelete = {
                            recordToDelete = record
                            showDeleteDialog = true
                        },
                        onShowInfo = {
                            recordForInfo = record
                            showInfoDialog = true
                        }
                    )
                }
            }

            // --- RENAME DIALOG ---
            if (showRenameDialog && recordToRename != null) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text(text = "Rename File", color = TextPrimary) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                label = { Text("File name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple,
                                    focusedLabelColor = PrimaryPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (renameText.isNotBlank()) {
                                    viewModel.renameFile(context, recordToRename!!, renameText.trim())
                                }
                                showRenameDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Rename")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

            // --- DELETE DIALOG ---
            if (showDeleteDialog && recordToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(text = "Delete History Record", color = TextPrimary) },
                    text = {
                        Column {
                            Text(
                                text = "Are you sure you want to delete this record from history?",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deletePhysicalFile = !deletePhysicalFile }
                                    .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = deletePhysicalFile,
                                    onCheckedChange = { deletePhysicalFile = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Also delete file from storage device",
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteRecord(recordToDelete!!, deletePhysicalFile)
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

            // --- INFO DIALOG ---
            if (showInfoDialog && recordForInfo != null) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = { Text(text = "File Metadata", color = TextPrimary) },
                    text = {
                        val record = recordForInfo!!
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InfoRow(label = "Filename", value = record.originalName)
                            InfoRow(label = "Format", value = record.targetFormat.uppercase())
                            InfoRow(label = "Source Size", value = formatSize(record.originalSize))
                            if (record.status == "SUCCESS") {
                                InfoRow(label = "Output Size", value = formatSize(record.outputSize))
                                InfoRow(label = "Duration", value = formatDuration(record.duration))
                                InfoRow(label = "Mime Type", value = record.mimeType)
                            }
                            InfoRow(label = "Timestamp", value = formatDate(record.timestamp))
                            InfoRow(
                                label = "Status",
                                value = record.status,
                                valueColor = when (record.status) {
                                    "SUCCESS" -> SuccessGreen
                                    "FAILED" -> ErrorRed
                                    else -> TextSecondary
                                }
                            )
                            if (!record.errorMessage.isNullOrEmpty()) {
                                InfoRow(label = "Error Details", value = record.errorMessage!!, valueColor = ErrorRed)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showInfoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Close")
                        }
                    },
                    containerColor = SurfaceDark
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    record: ConversionRecord,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = record.status == "SUCCESS", onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator Icon
            val (icon, color) = when (record.status) {
                "SUCCESS" -> {
                    if (record.mimeType.startsWith("audio/")) {
                        Pair(Icons.Rounded.Audiotrack, AccentCyan)
                    } else {
                        Pair(Icons.Rounded.PlayCircle, PrimaryPurple)
                    }
                }
                "FAILED" -> Pair(Icons.Rounded.ErrorOutline, ErrorRed)
                "CANCELLED" -> Pair(Icons.Rounded.CancelPresentation, TextSecondary)
                else -> Pair(Icons.Rounded.HourglassEmpty, TextSecondary)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = record.status,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File Information Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.originalName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (record.status == "SUCCESS") formatSize(record.outputSize) else record.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (record.status) {
                            "SUCCESS" -> TextSecondary
                            "FAILED" -> ErrorRed
                            else -> TextSecondary
                        }
                    )
                    if (record.status == "SUCCESS") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "•", fontSize = 12.sp, color = BorderColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatDate(record.timestamp),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // More Actions Dropdown Menu
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Actions",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SurfaceDark).border(1.dp, BorderColor)
                ) {
                    if (record.status == "SUCCESS") {
                        DropdownMenuItem(
                            text = { Text("Play / Open", color = TextPrimary) },
                            onClick = {
                                expanded = false
                                onPlay()
                            },
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, "Play", tint = AccentCyan) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", color = TextPrimary) },
                            onClick = {
                                expanded = false
                                onShare()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Share, "Share", tint = AccentCyan) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", color = TextPrimary) },
                            onClick = {
                                expanded = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Edit, "Rename", tint = AccentCyan) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Details Info", color = TextPrimary) },
                        onClick = {
                            expanded = false
                            onShowInfo()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Info, "Info", tint = AccentCyan) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorRed) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Delete, "Delete", tint = ErrorRed) }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column {
        Text(
            text = label.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
        Divider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(top = 6.dp))
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
