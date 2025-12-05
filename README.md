# REON Music App

A comprehensive, feature-rich Android music streaming app built with Kotlin and Jetpack Compose.

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
│   ├── common/        # Shared utilities & Result wrapper
│   ├── model/         # Data models (Song, Album, Artist, Playlist)
│   └── ui/            # Common UI components
├── data/
│   ├── network/       # API clients (JioSaavn, YouTube, LrcLib, SponsorBlock, RYD)
│   ├── database/      # Room database with DAOs
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

### Core Playback
- 🎵 Stream music from **JioSaavn** and **YouTube Music**
- 🎨 Beautiful Material 3 UI with **dynamic theming** (album art colors)
- 📱 Background playback with media controls
- 🎯 Gapless playback and crossfade
- 🔄 Queue management with shuffle and repeat
- ⏰ Sleep timer with fade-out

### Library & Organization
- 💾 Offline downloads with background worker
- 📋 Create and manage playlists
- ❤️ Liked songs collection
- 📊 Listening history and statistics
- 🎼 Full metadata support (lyrics, album art, artist info)

### Enhanced Search
- 🔍 Global search across JioSaavn and YouTube
- 📝 Search history
- 🏷️ Filter by type (All, Songs, Albums, Artists, Playlists)
- 🔢 Sort options (Relevance, Duration, Title, Date)

### Audio Features
- 🎚️ Audio equalizer with presets and custom band control
- 🎬 Video playback with Picture-in-Picture (PiP) mode
- 📺 Quality selection (360p, 720p, 1080p)
- 📝 Synchronized lyrics support (LrcLib integration)
- ⏩ **SponsorBlock** integration for auto-skipping segments

### Privacy & Customization
- 🔒 **Two build flavors**: FOSS (no tracking) and Full (optional crash reporting)
- 👤 Multi-account management with isolated preferences
- 🌙 Light, Dark, and AMOLED themes
- 🎨 Dynamic color theming from album artwork
- 🔄 YouTube Music bi-directional sync (playlists, liked songs, history)

### Cloud & Sync
- ☁️ Neon PostgreSQL cloud sync for cross-device library
- 🔄 YouTube Music integration (view likes/dislikes via RYD API)
- 📊 Statistics and "REON Wrapped" style analytics

### Android Integration
- 🚗 **Android Auto** support with MediaBrowserService
- 🎵 Audio format support: MP3, AAC/M4A, Opus, WebM
- 💾 Smart cache management (audio, images, lyrics)

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Clean Architecture
- **DI**: Hilt + Dagger
- **Networking**: Ktor Client
- **Database**: Room (local) + Neon PostgreSQL (cloud sync)
- **Playback**: Media3 (ExoPlayer)
- **Image Loading**: Coil
- **Background Tasks**: WorkManager
- **Preferences**: DataStore
- **Serialization**: kotlinx.serialization

## 📋 Requirements

- Android 8.0 (API 26) or higher
- Android Studio Hedgehog or later
- JDK 17 or OpenJDK 23

## 🏃 Building

### Build Flavors

The app has two product flavors:
- **FOSS**: No crash analytics, fully open-source friendly
- **Full**: Optional Sentry crash reporting (user consent required)

```bash
# Clone the repository
git clone https://github.com/your-username/reon-music.git

# Build FOSS debug APK
./gradlew assembleFossDebug

# Build Full debug APK (default)
./gradlew assembleFullDebug

# Build release APK with signing
./gradlew assembleFullRelease
```

### Configuration

Create `local.properties` in the project root:
```properties
sdk.dir=YOUR_ANDROID_SDK_PATH
```

For production builds, configure signing in `gradle.properties` or via environment variables.

## 🗂️ Database Schema

### Local Database (Room)
- `songs` - Song metadata and playback info
- `playlists` - User-created playlists
- `playlist_songs` - Playlist-song relationships
- `listen_history` - Play history with timestamps
- `artists` - Artist information
- `albums` - Album metadata
- `lyrics` - Cached lyrics

### Cloud Sync (Neon PostgreSQL)
- Bi-directional sync for library, playlists, and settings
- Conflict resolution with last-write-wins strategy
- OAuth-based authentication (planned)

## 📱 Screens

1. **Home** - Quick access, recommendations, recently played
2. **Search** - Global search with filters and history
3. **Library** - Liked songs, playlists, downloads, history
4. **Now Playing** - Full-screen player with lyrics
5. **Settings** - App preferences, themes, account management

## 🎨 Theming

- Light theme (default)
- Dark theme
- AMOLED black theme
- Dynamic theming from album artwork (uses Palette API)

## 🔐 Privacy

### FOSS Version
- ✅ No crash analytics
- ✅ No tracking or telemetry
- ✅ No third-party data collection
- ✅ Fully transparent data handling

### Full Version
- ⚠️ Optional Sentry crash reporting (requires user consent)
- ✅ No tracking or advertising
- ✅ User data stays local or in user-controlled Neon database
- ✅ YouTube sync only with explicit user permission

## 🌐 API Integrations

- **JioSaavn API**: Music streaming (undocumented public API)
- **YouTube InnerTube**: YouTube Music streaming
- **LrcLib**: Synchronized lyrics fetching
- **SponsorBlock**: Skip non-music segments
- **Return YouTube Dislike**: View like/dislike ratios
- **Neon PostgreSQL**: Cloud database for sync

## 📄 License

This project is proprietary software. All rights reserved.

## ⚠️ Disclaimer

This app is for **educational purposes** only. Users must:
- Have proper licensing agreements for any copyrighted music content
- Respect the terms of service of third-party APIs
- Not use this app to infringe on any copyrights

The developers do not endorse or encourage piracy in any form.

## 🤝 Contributing

This is a closed-source project. Contributions are not currently accepted.

## 📧 Contact

For inquiries: [your-email@example.com]

---

**Built with ❤️ using Kotlin and Jetpack Compose**

**Last Updated**: December 2024
**Version**: 1.0.0
**Build Status**: ✅ Passing (assembleFullDebug)
