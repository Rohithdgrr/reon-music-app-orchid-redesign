# REON Music App

A clean, modern Android music streaming app built with Kotlin and Jetpack Compose.

## ⚖️ Legal Notice

**CLEAN-ROOM IMPLEMENTATION**

This project is an **original, clean-room implementation**. No GPL-licensed code has been copied into this codebase. All code is independently written based on:

- Publicly documented Android APIs (Jetpack, Media3, Room, Hilt)
- Standard cryptographic algorithms (DES) available in Java SDK
- Publicly observable API behavior patterns

### Third-Party Dependencies

All dependencies use permissive licenses (Apache 2.0, MIT):
- Jetpack Compose, Media3, Room, Hilt - Apache 2.0
- Ktor - Apache 2.0
- Coil - Apache 2.0
- Kotlin Serialization - Apache 2.0

## 🏗️ Architecture

Multi-module architecture following clean architecture principles:

```
app/                    # Main application module
├── core/
│   ├── common/        # Shared utilities
│   ├── model/         # Data models
│   └── ui/            # Common UI components
├── data/
│   ├── network/       # API clients (JioSaavn, YouTube)
│   ├── database/      # Room database
│   └── repository/    # Data repositories
├── media/
│   └── playback/      # Media3 playback service
└── feature/
    ├── home/          # Home screen
    ├── search/        # Search screen
    ├── player/        # Now playing screen
    ├── library/       # Library screen
    └── settings/      # Settings screen
```

## 🚀 Features

- 🎵 Stream music from JioSaavn and YouTube Music
- 🎨 Clean, modern light theme UI
- 📱 Background playback with media controls
- 💾 Offline downloads and caching
- 📋 Create and manage playlists
- 🔍 Powerful search with filters
- ⚡ Gapless playback and crossfade

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Clean Architecture
- **DI**: Hilt
- **Networking**: Ktor
- **Database**: Room
- **Playback**: Media3 (ExoPlayer)
- **Image Loading**: Coil

## 📋 Requirements

- Android 8.0 (API 26) or higher
- Android Studio Hedgehog or later
- JDK 17

## 🏃 Building

```bash
# Clone the repository
git clone https://github.com/your-username/reon-music.git

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## 📄 License

This project is proprietary software. See LICENSE file for details.

---

**Note**: This app is for educational purposes. Ensure you have proper licensing agreements before distributing copyrighted music content.
