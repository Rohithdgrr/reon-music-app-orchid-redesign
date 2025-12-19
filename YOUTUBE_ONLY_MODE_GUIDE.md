# REON Music App - YouTube-Only Mode & UI Updates

## Summary of Requested Changes

### ✅ **COMPLETED:**

1. **JioSaavn Disabled** - Commented out JioSaavn search functionality
2. **YouTube Music Only Mode** - App now uses only YouTube Music as source

### 🔄 **REMAINING TASKS:**

## 1. Remove Telugu/Hindi/Tamil Playlists from Home Screen

**File:** `app/src/main/java/com/reon/music/ui/screens/HomeScreen.kt`

**Action:** Remove these three playlist sections:
- Telugu Playlists (2000s, 1990s)
- Hindi Playlists (1990s, 2000s)
- Tamil Playlists

**Implementation:**
```kotlin
// Remove or comment out:
// TeluguPlaylistsSection()
// HindiPlaylistsSection()
// TamilPlaylistsSection()
```

---

## 2. Update Player UI - White Background with Thumbnail

**File:** `app/src/main/java/com/reon/music/ui/screens/NowPlayingScreen.kt`

**Changes Needed:**
```kotlin
// Change from current red background to white
Surface(
    color = MaterialTheme.colorScheme.surface, // White background
    modifier = Modifier.fillMaxSize()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prominent album art/thumbnail
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Song title & artist on white background
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
        
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}
```

---

## 3. Update/Remove Filters on Home Screen

**File:** `app/src/main/java/com/reon/music/ui/screens/HomeScreen.kt`

**Current:** Filter pills (All, Telugu, Hindi, Tamil)

**Recommended:** Replace with cleaner design:
```kotlin
// Option 1: Remove completely
// Comment out the Filters section

// Option 2: Simplify to 2-3 genres
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    FilterChip(
        selected = selectedFilter == "All",
        onClick = { selectedFilter = "All" },
        label = { Text("All Music") }
    )
    FilterChip(
        selected = selectedFilter == "Trending",
        onClick = { selectedFilter = "Trending" },
        label = { Text("Trending") }
    )
}
```

---

## 4. Replace "Top Artists" with Curated List

**File:** `app/src/main/java/com/reon/music/ui/screens/HomeScreen.kt`

**Implementation:**

```kotlin
// Top Artists Data Class
data class FeaturedArtist(
    val name: String,
    val genre: String,
    val category: String // Telugu, Hindi, Tamil, Pan-Indian, International
)

// Curated Artist List
val featuredArtists = listOf(
    // Telugu
    FeaturedArtist("M.M. Keeravani", "Music Director", "Telugu"),
    FeaturedArtist("Devi Sri Prasad", "Music Director", "Telugu"),
    FeaturedArtist("Sid Sriram", "Singer", "Telugu"),
    FeaturedArtist("Sunitha Upadrashta", "Singer", "Telugu"),
    
    // Hindi
    FeaturedArtist("A.R. Rahman", "Music Director", "Hindi"),
    FeaturedArtist("Pritam", "Music Director", "Hindi"),
    FeaturedArtist("Arijit Singh", "Singer", "Hindi"),
    FeaturedArtist("Shreya Ghoshal", "Singer", "Hindi"),
    FeaturedArtist("Sonu Nigam", "Singer", "Hindi"),
    
    // Tamil
    FeaturedArtist("Ilaiyaraaja", "Music Director", "Tamil"),
    FeaturedArtist("A.R. Rahman", "Music Director", "Tamil"),
    FeaturedArtist("Anirudh Ravichander", "Music Director", "Tamil"),
    
    // Pan-Indian
    FeaturedArtist("Ajay-Atul", "Music Director", "Pan-Indian"),
    FeaturedArtist("Shankar Mahadevan", "Singer", "Pan-Indian"),
    
    // International
    FeaturedArtist("Hans Zimmer", "Composer", "International"),
    FeaturedArtist("Taylor Swift", "Singer", "International"),
    FeaturedArtist("Ed Sheeran", "Singer", "International")
)

// UI Component
@Composable
fun FeaturedArtistsSection(artists: List<FeaturedArtist> = featuredArtists) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Featured Artists",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists) { artist ->
                ArtistCard(artist)
            }
        }
    }
}

@Composable
fun ArtistCard(artist: FeaturedArtist) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular avatar placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = artist.genre,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

---

## 5. Update Splash Screen

**File:** `app/src/main/res/drawable/splash_background.xml` or update in code

**Action:** Use the image at:
`C:\Users\rohit\Music\REON LIBRA app using flutter first edition\project app 1\REONm\splash screen.jpeg`

**Steps:**
1. Copy `splash screen.jpeg` to `app/src/main/res/drawable/`
2. Rename to `splash_screen.jpg`  
3. Update splash theme or SplashActivity:

```kotlin
// In SplashActivity or MainActivity
setContent {
    Image(
        painter = painterResource(R.drawable.splash_screen),
        contentDescription = "Splash Screen",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
```

**Or in themes.xml:**
```xml
<item name="android:windowBackground">@drawable/splash_screen</item>
```

---

## 🎯 **QUICK IMPLEMENTATION CHECKLIST**

### Priority 1 (High Impact):
- [ ] Remove Telugu/Hindi/Tamil playlists from HomeScreen
- [ ] Update Player UI to white background with thumbnail
- [ ] Add splash screen image

### Priority 2 (Medium Impact):
- [ ] Replace Top Artists with curated list
- [ ] Update/simplify filter design

### Priority 3 (Code Cleanup):
- [ ] Fully comment out all JioSaavn references
- [ ] Update repository methods documentation
- [ ] Test YouTube-only mode thoroughly

---

## 📱 **EXPECTED FINAL UI**

### Home Screen:
```
┌─────────────────────────┐
│ REON MUSIC             │
│ Good Afternoon          │
│                         │
│ [Simplified Filters]    │
│                         │
│ Featured Artists ───▶   │
│ ┌───┐ ┌───┐ ┌───┐     │
│ │ AR│ │DSP│ │Sid│     │
│ └───┘ └───┘ └───┘     │
│                         │
│ Trending ───────────▶   │
│ [Song Cards]            │
└─────────────────────────┘
```

### Player Screen:
```
┌─────────────────────────┐
│     ▼   NOW PLAYING    │
│                         │
│    ┌──────────────┐    │
│    │              │    │
│    │   Album      │    │
│    │   Artwork    │    │
│    │              │    │
│    └──────────────┘    │
│                         │
│    Song Title           │
│    Artist Name          │
│                         │
│    ────●────────        │
│    2:30      4:00       │
│                         │
│    ◄◄    ▶    ►►       │
└─────────────────────────┘
```

---

## 🔧 **FILES TO MODIFY**

1. `data/repository/MusicRepository.kt` - ✅ DONE (JioSaavn disabled)
2. `app/src/main/java/com/reon/music/ui/screens/HomeScreen.kt` - Remove playlists, add curated artists
3. `app/src/main/java/com/reon/music/ui/screens/NowPlayingScreen.kt` - White background design
4. `app/src/main/res/drawable/` - Add splash screen image
5. `app/src/main/res/values/themes.xml` - Update splash configuration

---

## ⚙️ **CONFIGURATION CHANGES**

### In `build.gradle.kts` (if needed):
```kotlin
// Ensure YouTube Music dependencies are prioritized
dependencies {
    // YouTube Music (Primary)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    
    // JioSaavn (Disabled - keep for future)
    // implementation(...)
}
```

---

**Status:** ✅ JioSaavn Disabled, YouTube-Only Mode Active  
**Next:** UI updates as per checklist above  
**Testing:** Build and verify YouTube Music search/playback works

Would you like me to proceed with implementing any specific section from this plan?
