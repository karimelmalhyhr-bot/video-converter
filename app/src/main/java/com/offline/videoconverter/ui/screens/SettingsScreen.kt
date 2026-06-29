package com.offline.videoconverter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.videoconverter.MainViewModel
import com.offline.videoconverter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Selected states
    var mode by remember { mutableStateOf("VIDEO") } // "VIDEO" or "AUDIO"
    var targetFormat by remember { mutableStateOf("mp4") } // "mp4", "mkv", "mov", "webm"
    var videoQuality by remember { mutableStateOf("Medium") } // "Low", "Medium", "High", "Original"
    var audioBitrate by remember { mutableStateOf("192") } // "64", "128", "192", "320"

    val videoName by viewModel.selectedVideoName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configure Job",
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
                    .size(250.dp)
                    .align(Alignment.Center)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryPurple.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Source name banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoFile,
                            contentDescription = "Source",
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Source: $videoName",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mode Selection Tab (Segment Control)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mode == "VIDEO") PrimaryPurple else Color.Transparent)
                                .clickable {
                                    mode = "VIDEO"
                                    targetFormat = "mp4" // Reset format if needed
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Convert Video",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == "VIDEO") Color.White else TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mode == "AUDIO") PrimaryPurple else Color.Transparent)
                                .clickable {
                                    mode = "AUDIO"
                                    targetFormat = "mp3"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Extract Audio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == "AUDIO") Color.White else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Dynamic Configurations based on Mode
                    if (mode == "VIDEO") {
                        // VIDEO TARGET FORMAT
                        Text(
                            text = "TARGET FORMAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val formats = listOf("mp4", "mkv", "mov", "webm")
                            formats.forEach { format ->
                                val selected = targetFormat == format
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) PrimaryPurple.copy(alpha = 0.2f) else SurfaceDark)
                                        .border(
                                            1.5.dp,
                                            if (selected) PrimaryPurple else BorderColor,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { targetFormat = format },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = format.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // VIDEO QUALITY PRESET
                        Text(
                            text = "OUTPUT QUALITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val qualities = listOf("Low", "Medium", "High", "Original")
                            qualities.forEach { q ->
                                val selected = videoQuality == q
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) PrimaryPurple.copy(alpha = 0.2f) else SurfaceDark)
                                        .border(
                                            1.5.dp,
                                            if (selected) PrimaryPurple else BorderColor,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { videoQuality = q },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = q,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        // AUDIO MODE (MP3 Bitrate)
                        Text(
                            text = "AUDIO BITRATE (MP3)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val bitrates = listOf("64", "128", "192", "320")
                            bitrates.forEach { br ->
                                val selected = audioBitrate == br
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) PrimaryPurple.copy(alpha = 0.2f) else SurfaceDark)
                                        .border(
                                            1.5.dp,
                                            if (selected) PrimaryPurple else BorderColor,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { audioBitrate = br },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$br kbps",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Summary & Submit Button
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Nice descriptive pill
                    Text(
                        text = if (mode == "VIDEO") {
                            "Will encode video to $targetFormat with $videoQuality quality locally."
                        } else {
                            "Will extract MP3 audio at $audioBitrate kbps bitrate."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.startConversion(
                                mode = mode,
                                targetFormat = targetFormat,
                                qualityOrBitrate = if (mode == "VIDEO") videoQuality else audioBitrate
                            )
                            onNavigateToHistory()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(PrimaryPurple, SecondaryViolet)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsBackupRestore,
                                contentDescription = "Convert",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Conversion",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
