# REON Music App - Architecture Overview

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        USER INTERFACE                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Home    │  │  Search  │  │ Library  │  │ Settings │   │
│  │  Screen  │  │  Screen  │  │  Screen  │  │  Screen  │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       │             │              │             │          │
│       └─────────────┴──────────────┴─────────────┘          │
│                          │                                   │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                    THEME SYSTEM                               │
├──────────────────────────┼───────────────────────────────────┤
│                          │                                    │
│  ┌───────────────────────▼─────────────────────────┐        │
│  │           ReonTheme Composable                   │        │
│  │  ┌────────────────────────────────────────┐     │        │
│  │  │  Theme Preset Selection                 │     │        │
│  │  │  • Classic Green  • Ocean Blue          │     │        │
│  │  │  • Sunset Orange  • Purple Haze         │     │        │
│  │  │  • Rose Gold      • Forest Green        │     │        │
│  │  │  • Midnight Black • Crimson Red         │     │        │
│  │  └────────────────────────────────────────┘     │        │
│  │                                                   │        │
│  │  ┌────────────────────────────────────────┐     │        │
│  │  │  Font Customization                     │     │        │
│  │  │  • Font Family (8 options)              │     │        │
│  │  │  • Font Size (4 sizes)                  │     │        │
│  │  │  • Dynamic Typography Scaling           │     │        │
│  │  └────────────────────────────────────────┘     │        │
│  │                                                   │        │
│  │  ┌────────────────────────────────────────┐     │        │
│  │  │  Color Scheme Generation                │     │        │
│  │  │  • Light Mode Colors                    │     │        │
│  │  │  • Dark Mode Colors                     │     │        │
│  │  │  • AMOLED Mode (Pure Black)             │     │        │
│  │  │  • Dynamic Colors (from album art)      │     │        │
│  │  └────────────────────────────────────────┘     │        │
│  └───────────────────────────────────────────────┘        │
│                          │                                    │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                   VIEW MODELS                                 │
├──────────────────────────┼───────────────────────────────────┤
│                          │                                    │
│  ┌───────────────────────▼─────────────────────────┐        │
│  │         SettingsViewModel                        │        │
│  │  ┌────────────────────────────────────────┐     │        │
│  │  │  Theme Preferences                      │     │        │
│  │  │  • themePresetId: String?               │     │        │
│  │  │  • fontPresetId: String?                │     │        │
│  │  │  • fontSizePreset: FontSizePreset       │     │        │
│  │  │  • dynamicColors: Boolean               │     │        │
│  │  └────────────────────────────────────────┘     │        │
│  │                                                   │        │
│  │  ┌────────────────────────────────────────┐     │        │
│  │  │  Auto-Update Settings                   │     │        │
│  │  │  • autoUpdateEnabled: Boolean           │     │        │
│  │  │  • autoUpdateFrequency: Int             │     │        │
│  │  │  • autoUpdateWifiOnly: Boolean          │     │        │
│  │  └────────────────────────────────────────┘     │        │
│  │                                                   │        │
│  │  ┌────────────────────────────────────────┐     │        │
│  │  │  Methods                                │     │        │
│  │  │  • setThemePreset(id)                   │     │        │
│  │  │  • setFontPreset(id)                    │     │        │
│  │  │  • setFontSize(preset)                  │     │        │
│  │  │  • setAutoUpdateEnabled(enabled)        │     │        │
│  │  └────────────────────────────────────────┘     │        │
│  └───────────────────────────────────────────────┘        │
│                          │                                    │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                  DATA LAYER                                   │
├──────────────────────────┼───────────────────────────────────┤
│                          │                                    │
│  ┌───────────────────────▼─────────────────────────┐        │
│  │         UserPreferences (DataStore)              │        │
│  │  • Save theme preset                             │        │
│  │  • Save font preset                              │        │
│  │  • Save font size                                │        │
│  │  • Save auto-update settings                     │        │
│  └───────────────────────┬─────────────────────────┘        │
│                          │                                    │
│  ┌───────────────────────▼─────────────────────────┐        │
│  │         ContentSyncWorker (WorkManager)          │        │
│  │  • Periodic background sync                      │        │
│  │  • Update charts, playlists, new releases        │        │
│  │  • Respect WiFi-only setting                     │        │
│  │  • Send update notifications                     │        │
│  └───────────────────────┬─────────────────────────┘        │
│                          │                                    │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                  BACKEND SERVICES                             │
├──────────────────────────┼───────────────────────────────────┤
│                          │                                    │
│  ┌───────────────────────▼─────────────────────────┐        │
│  │         YouTube Music API                        │        │
│  │  • Fetch charts                                  │        │
│  │  • Fetch playlists                               │        │
│  │  • Fetch new releases                            │        │
│  │  • Stream music                                  │        │
│  └──────────────────────────────────────────────────┘        │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

## 🔄 Data Flow

### Theme Selection Flow

```
User taps "Theme Preset" in Settings
         ↓
ThemePresetSelector dialog opens
         ↓
User selects "Ocean Blue" theme
         ↓
onPresetSelected("ocean_blue") called
         ↓
SettingsViewModel.setThemePreset("ocean_blue")
         ↓
_uiState.value updated with new themePresetId
         ↓
ReonApp observes uiState change
         ↓
ReonTheme receives themePresetId = "ocean_blue"
         ↓
ThemePresets.getPresetById("ocean_blue") returns OceanBlue preset
         ↓
Color scheme extracted (light or dark based on system)
         ↓
MaterialTheme updated with new colors
         ↓
Animated color transitions (300ms)
         ↓
Entire app reflects Ocean Blue theme! 🌊
```

### Font Selection Flow

```
User taps "Font Family" in Settings
         ↓
FontPresetSelector dialog opens
         ↓
User selects "Poppins" font
         ↓
onFontSelected("poppins") called
         ↓
SettingsViewModel.setFontPreset("poppins")
         ↓
_uiState.value updated with new fontPresetId
         ↓
ReonApp observes uiState change
         ↓
ReonTheme receives fontPresetId = "poppins"
         ↓
FontPresets.getPresetById("poppins") returns Poppins preset
         ↓
FontPresets.createTypography(Poppins.fontFamily, sizePreset)
         ↓
Typography with Poppins font generated
         ↓
MaterialTheme updated with new typography
         ↓
All text in app updates to Poppins font! ✍️
```

### Auto-Update Flow

```
User enables "Auto-Update" in Settings
         ↓
SettingsViewModel.setAutoUpdateEnabled(true)
         ↓
WorkManager schedules ContentSyncWorker
         ↓
Worker runs every X minutes (based on frequency setting)
         ↓
Check if WiFi-only enabled
         ↓
If WiFi-only: Wait for WiFi connection
         ↓
Fetch latest charts from YouTube Music API
         ↓
Fetch latest playlists
         ↓
Fetch new releases
         ↓
Update local database
         ↓
Update lastSyncTime
         ↓
Send notification if new content available
         ↓
UI automatically reflects updated content! 🔄
```

## 📊 Component Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                      ReonApp                                 │
│  (Main application composable)                               │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ReonTheme                                           │   │
│  │  • Receives theme/font preferences from Settings     │   │
│  │  • Applies MaterialTheme with custom colors/fonts    │   │
│  │  • Wraps entire app content                          │   │
│  │                                                       │   │
│  │  ┌───────────────────────────────────────────────┐  │   │
│  │  │  Navigation (NavHost)                         │  │   │
│  │  │                                               │  │   │
│  │  │  ┌─────────────────────────────────────────┐ │  │   │
│  │  │  │  HomeScreen                             │ │  │   │
│  │  │  │  • Displays charts, playlists, songs    │ │  │   │
│  │  │  │  • Uses theme colors                    │ │  │   │
│  │  │  │  • Uses custom typography               │ │  │   │
│  │  │  └─────────────────────────────────────────┘ │  │   │
│  │  │                                               │  │   │
│  │  │  ┌─────────────────────────────────────────┐ │  │   │
│  │  │  │  SettingsScreen                         │ │  │   │
│  │  │  │  • Theme selector                       │ │  │   │
│  │  │  │  • Font selector                        │ │  │   │
│  │  │  │  • Auto-update settings                 │ │  │   │
│  │  │  │  • Triggers preference updates          │ │  │   │
│  │  │  └─────────────────────────────────────────┘ │  │   │
│  │  │                                               │  │   │
│  │  │  ┌─────────────────────────────────────────┐ │  │   │
│  │  │  │  Other Screens                          │ │  │   │
│  │  │  │  • Search, Library, Downloads, etc.     │ │  │   │
│  │  │  │  • All inherit theme/font settings      │ │  │   │
│  │  │  └─────────────────────────────────────────┘ │  │   │
│  │  └───────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🎨 Theme System Details

### Theme Preset Structure

```kotlin
ThemePreset {
    id: "ocean_blue"
    name: "Ocean Blue"
    emoji: "🌊"
    description: "Calm and professional"
    
    lightScheme: ColorScheme {
        primary: #0077BE (Blue)
        secondary: #00ACC1 (Teal)
        background: #F8FBFF (Light Blue)
        surface: #FFFFFF (White)
        // ... more colors
    }
    
    darkScheme: ColorScheme {
        primary: #4FC3F7 (Light Blue)
        secondary: #4DD0E1 (Light Teal)
        background: #0A1929 (Dark Blue)
        surface: #132F4C (Navy)
        // ... more colors
    }
}
```

### Font Preset Structure

```kotlin
FontPreset {
    id: "poppins"
    name: "Poppins"
    fontFamily: FontFamily(Font(R.font.poppins_regular))
    description: "Geometric with personality"
}

FontSizePreset {
    SMALL: 0.85x scale
    MEDIUM: 1.0x scale (default)
    LARGE: 1.15x scale
    XLARGE: 1.3x scale
}
```

## 🔧 Settings State Management

```kotlin
SettingsUiState {
    // Theme
    theme: AppTheme = SYSTEM
    themePresetId: String? = null
    pureBlack: Boolean = false
    dynamicColors: Boolean = true
    
    // Font
    fontPresetId: String? = null
    fontSizePreset: FontSizePreset = MEDIUM
    
    // Auto-Update
    autoUpdateEnabled: Boolean = true
    autoUpdateFrequency: Int = 60 // minutes
    autoUpdateWifiOnly: Boolean = true
    lastSyncTime: Long = 0
    
    // ... other settings
}
```

## 📱 User Journey

### First Time User

```
1. Opens app → Sees default theme (Classic Green)
2. Explores music → Likes the app
3. Opens Settings → Discovers personalization options
4. Tries different themes → Finds favorite (e.g., Purple Haze)
5. Adjusts font size → Improves readability
6. Enables auto-update → Gets fresh content daily
7. Enjoys personalized experience! 🎉
```

### Power User

```
1. Changes theme based on mood
   • Morning: Ocean Blue (calm)
   • Afternoon: Sunset Orange (energetic)
   • Evening: Midnight Black (battery saving)

2. Customizes for different activities
   • Reading lyrics: Large font size
   • Browsing: Medium font size
   • Exercising: Bold font (Montserrat)

3. Optimizes performance
   • Enables WiFi-only updates
   • Sets cache limits
   • Uses data saver mode

4. Maximizes experience! 🚀
```

---

## 📈 Performance Considerations

### Memory Usage

```
Theme System:
• Presets: ~10KB (static data)
• Color schemes: ~2KB per theme
• Typography: ~5KB per font
• Total overhead: <50KB

Font System:
• Font files: ~100-200KB each (if custom fonts added)
• Typography cache: ~10KB
• Minimal runtime overhead

Auto-Update:
• WorkManager: ~5MB (system service)
• Sync data: ~1-5MB per sync
• Cached content: User configurable
```

### Battery Impact

```
Theme switching: Negligible
Font changes: Negligible
Auto-update (hourly): ~2-3% per day
Dynamic colors: ~1% per day
```

---

**Architecture designed for:**
✅ Scalability
✅ Maintainability  
✅ Performance
✅ User Experience
✅ Future Enhancements

---

*Created: December 7, 2024*
*Version: 1.1.0*
