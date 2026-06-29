# Offline Video Converter & Audio Extractor

A free, 100% offline-first, performance-optimized, and private native Android application that enables users to convert video files between multiple formats (MP4, MKV, MOV, WEBM) and extract high-quality MP3 audio from videos local on-device.

---

## Key Features
- **100% Offline-First**: Zero internet permissions requested in `AndroidManifest.xml`. No web API calls, analytics, trackers, or ads. Completely private.
- **Video Conversion**: Transcode video files locally between formats (MP4, MKV, MOV, WEBM) with selectable quality presets (Low, Medium, High, Original).
- **Audio Extraction**: Strip audio channels from imported videos and encode them to MP3 at custom bitrates (64, 128, 192, 320 kbps).
- **Background Support**: Managed via a Foreground Service (`ConversionService`) displaying real-time transcode speed, completion progress, and a "Cancel" action. Conversions persist even if the app is minimized.
- **Sleek UI**: Built using Jetpack Compose with a modern neon-violet dark theme, smooth slide animations, and clear error boundaries.
- **Scoped Storage Compliant**: Uses Android's native `MediaStore` API. On Android 10+ (API 29+), it writes completed files to the public `Movies/VideoConverter` and `Music/VideoConverter` directories **without requiring any runtime storage permissions**.

---

## Technical Stack & Architecture

```
e:\video converter
├── settings.gradle               # Gradle build hierarchy
├── build.gradle                  # Root build plugins
├── gradle.properties             # Build options
└── app
    ├── build.gradle              # App modules & dependencies (FFmpeg-Kit, Room, Compose)
    └── src
        └── main
            ├── AndroidManifest.xml
            ├── res/              # Vector icons, themes, and strings
            └── java/com/offline/videoconverter
                ├── VideoConverterApp.kt   # App configuration & notification channels
                ├── MainActivity.kt        # Entry Point, dynamic permission handling & router
                ├── MainViewModel.kt       # Application State manager & file handlers
                ├── data/
                │   ├── AppDatabase.kt     # Room database wrapper
                │   ├── ConversionDao.kt   # Database access queries
                │   ├── ConversionRecord.kt# Conversion job history schema
                │   ├── ConversionState.kt # UI progress states container
                │   └── ConversionManager.kt # Singleton coordinator between service and UI
                ├── service/
                │   └── ConversionService.kt # Foreground service executing FFmpeg commands
                └── ui/
                    ├── theme/
                    │   ├── Color.kt       # Design tokens (Neon purples & custom deep colors)
                    │   └── Theme.kt       # Theme wrapper
                    └── screens/
                        ├── HomeScreen.kt  # Selection interface & file metadata reader
                        ├── SettingsScreen.kt # Format picker and bitrate configurations
                        └── HistoryScreen.kt # Logs, player launcher, shares, renames & deletions
```

### Conversion Engine
The conversion is powered by **FFmpeg-Kit (Minimal)** (`com.arthenica:ffmpeg-kit-min:6.0-2`), which bundles optimized software decoders/encoders. Because it runs purely in software:
- It works consistently across **all** devices, avoiding the inconsistencies of Android hardware encoders.
- The `min` package is selected to optimize APK size, retaining encoders for `libx264` (H.264), `libvpx` (VP8), `libvorbis` (Vorbis), and `lame` (MP3).

---

## Build & Run Instructions

### Prerequisites
- **Android Studio Jellyfish / Koala** (or newer).
- **JDK 17** configured as the Gradle JDK in Android Studio (`Settings > Build, Execution, Deployment > Build Tools > Gradle`).
- An Android Emulator or physical device running **Android 8.0 (API 26)** or higher.

### Steps to Run
1. **Clone or Open the Project**:
   Open Android Studio, select **Open**, and navigate to the project directory (`e:\video converter`).
2. **Gradle Sync**:
   Let Android Studio download dependencies (FFmpeg-Kit, Jetpack Compose, Room DB) and index the codebase.
3. **Build APK**:
   Select `Build > Build Bundle(s) / APK(s) > Build APK(s)` to compile a local debug/release build.
4. **Deploy & Run**:
   Connect your test device/emulator and press the green **Run** button in Android Studio.

---

## Storage & File Organization
- Converted videos are written to `/storage/emulated/0/Movies/VideoConverter/`
- Extracted audio files are written to `/storage/emulated/0/Music/VideoConverter/`
- Both target directories are registered with the system scanner immediately after completion, meaning files will display inside standard Galleries and Music Players instantly.
