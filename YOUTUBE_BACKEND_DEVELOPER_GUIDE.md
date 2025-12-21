# YouTube Music Backend - Developer Guide

## 🚀 Quick Start

### Accessing Rich Metadata

```kotlin
// In your ViewModel or Repository
val song = youtubeMusicClient.getEnhancedSongDetails(videoId).getOrNull()

// Access new fields
song?.let {
    val composer = it.extras["composer"]
    val lyricist = it.extras["lyricist"]
    val producer = it.extras["producer"]
    val musicLabel = it.extras["musicLabel"]
    val movieName = it.extras["movieName"]
    val year = it.year
    val album = it.album
    val description = it.description
}
```

---

## 📋 Available Metadata Fields

### Core Fields (Direct Access)
- `song.title` - Song title
- `song.artist` - Artist/channel name
- `song.album` - Album or movie name
- `song.year` - Release year
- `song.description` - Video description
- `song.viewCount` - View count
- `song.likeCount` - Like count
- `song.channelName` - YouTube channel
- `song.channelId` - Channel ID
- `song.uploadDate` - Upload date

### Extended Metadata (via `extras` map)
- `extras["composer"]` - Music composer
- `extras["lyricist"]` - Song lyricist
- `extras["producer"]` - Music producer
- `extras["musicLabel"]` - Record label
- `extras["movieName"]` - Movie/film name (Bollywood)
- `extras["singer"]` - Singer name

---

## 💡 Usage Examples

### Example 1: Display Song Credits in UI

```kotlin
@Composable
fun SongDetailsScreen(song: Song) {
    Column {
        Text(song.title, style = MaterialTheme.typography.headlineMedium)
        Text(song.artist, style = MaterialTheme.typography.bodyLarge)
        
        // Show movie name if available (Bollywood songs)
        song.extras["movieName"]?.let { movie ->
            Row {
                Icon(Icons.Default.Movie, contentDescription = null)
                Text("From: $movie")
            }
        }
        
        // Show composer
        song.extras["composer"]?.let { composer ->
            Text("Music: $ composer", style = MaterialTheme.typography.bodyMedium)
        }
        
        // Show lyricist
        song.extras["lyricist"]?.let { lyricist ->
            Text("Lyrics: $lyricist", style = MaterialTheme.typography.bodyMedium)
        }
        
        // Show music label
        song.extras["musicLabel"]?.let { label ->
            Text("Label: $label", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

### Example 2: Search by Composer

```kotlin
suspend fun searchByComposer(composerName: String): List<Song> {
    val allSongs = musicRepository.searchSongs(composerName).getOrNull() ?: emptyList()
    
    // Enhance with metadata
    val enrichedSongs = musicRepository.enhanceSongsWithMetadata(allSongs)
    
    // Filter by exact composer match
    return enrichedSongs.filter { song ->
        song.extras["composer"]?.contains(composerName, ignoreCase = true) == true
    }
}

// Usage
val arRahmanSongs = searchByComposer("AR Rahman")
val mithoonSongs = searchByComposer("Mithoon")
```

### Example 3: Create Movie-Based Playlists

```kotlin
suspend fun getSongsFromMovie(movieName: String): List<Song> {
    val searchResults = musicRepository.searchSongs(movieName).getOrNull() ?: emptyList()
    val enriched = musicRepository.enhanceSongsWithMetadata(searchResults)
    
    return enriched.filter { song ->
        song.extras["movieName"]?.contains(movieName, ignoreCase = true) == true ||
        song.album.contains(movieName, ignoreCase = true)
    }
}

// Usage
val aashiqui2Playlist = getSongsFromMovie("Aashiqui 2")
val yjhdPlaylist = getSongsFromMovie("Yeh Jawaani Hai Deewani")
```

### Example 4: Smart Filtering by Multiple Criteria

```kotlin
data class MusicFilter(
    val composer: String? = null,
    val lyricist: String? = null,
    val movieName: String? = null,
    val year: String? = null,
    val musicLabel: String? = null
)

suspend fun filterSongs(query: String, filter: MusicFilter): List<Song> {
    val songs = musicRepository.searchSongs(query).getOrNull() ?: emptyList()
    val enriched = musicRepository.enhanceSongsWithMetadata(songs)
    
    return enriched.filter { song ->
        (filter.composer == null || song.extras["composer"]?.contains(filter.composer, true) == true) &&
        (filter.lyricist == null || song.extras["lyricist"]?.contains(filter.lyricist, true) == true) &&
        (filter.movieName == null || song.extras["movieName"]?.contains(filter.movieName, true) == true) &&
        (filter.year == null || song.year == filter.year) &&
        (filter.musicLabel == null || song.extras["musicLabel"]?.contains(filter.musicLabel, true) == true)
    }
}

// Usage
val romanticMithoonSongs = filterSongs(
    query = "romantic songs",
    filter = MusicFilter(composer = "Mithoon")
)
```

### Example 5: Analytics Dashboard

```kotlin
suspend fun getMusicStatistics(songs: List<Song>) {
    val enriched = musicRepository.enhanceSongsWithMetadata(songs)
    
    // Top composers
    val topComposers = enriched
        .mapNotNull { it.extras["composer"] }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(10)
    
    // Top music labels
    val topLabels = enriched
        .mapNotNull { it.extras["musicLabel"] }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(10)
    
    // Movies with most songs
    val topMovies = enriched
        .mapNotNull { it.extras["movieName"] }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(10)
    
    println("Top Composers: $topComposers")
    println("Top Labels: $topLabels")
    println("Top Movies: $topMovies")
}
```

---

## 🎨 UI Components

### Song Credit Badge Component

```kotlin
@Composable
fun SongCreditBadge(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Usage
@Composable
fun SongCredits(song: Song) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        song.extras["composer"]?.let {
            SongCreditBadge(icon = Icons.Default.MusicNote, label = "Composer", value = it)
        }
        song.extras["lyricist"]?.let {
            SongCreditBadge(icon = Icons.Default.Edit, label = "Lyricist", value = it)
        }
        song.extras["producer"]?.let {
            SongCreditBadge(icon = Icons.Default.Person, label = "Producer", value = it)
        }
        song.extras["musicLabel"]?.let {
            SongCreditBadge(icon = Icons.Default.Album, label = "Label", value = it)
        }
        song.extras["movieName"]?.let {
            SongCreditBadge(icon = Icons.Default.Movie, label = "Movie", value = it)
        }
    }
}
```

---

## 🔍 Advanced Features

### Automatic Metadata Enhancement

```kotlin
class EnhancedMusicRepository @Inject constructor(
    private val youtubeMusicClient: YouTubeMusicClient,
    private val songDao: SongDao
) {
    /**
     * Automatically enhance and cache songs
     */
    suspend fun getEnhancedSongs(query: String): Flow<List<Song>> = flow {
        // Immediate results
        val basicSongs = searchSongs(query).getOrNull() ?: emptyList()
        emit(basicSongs)
        
        // Enhanced results
        val enriched = enhanceSongsWithMetadata(basicSongs)
        emit(enriched)
        
        // Cache for offline access
        songDao.insertAll(enriched)
    }
}
```

### Metadata Validation

```kotlin
fun Song.hasCompleteMetadata(): Boolean {
    return extras["composer"] != null &&
           extras["lyricist"] != null &&
           extras["musicLabel"] != null &&
           year.isNotBlank()
}

fun List<Song>.filterWithMetadata(): List<Song> {
    return filter { it.hasCompleteMetadata() }
}
```

---

## ⚡ Performance Tips

1. **Cache Enhanced Metadata**
   ```kotlin
   // Cache enriched songs to avoid repeated API calls
   val enrichedSongs = musicRepository.enhanceSongsWithMetadata(songs)
   songDao.insertAll(enrichedSongs)
   ```

2. **Lazy Loading**
   ```kotlin
   // Load basic info first, enhance on demand
   fun loadSongDetails(videoId: String): Flow<Song?> = flow {
       // Quick basic load
       val basic = youtubeMusicClient.getSongDetails(videoId).getOrNull()
       emit(basic)
       
       // Enhanced metadata
       val enhanced = youtubeMusicClient.getEnhancedSongDetails(videoId).getOrNull()
       emit(enhanced)
   }
   ```

3. **Batch Processing**
   ```kotlin
   // Process songs in batches to avoid overwhelming the API
   suspend fun enhanceSongsBatch(songs: List<Song>, batchSize: Int = 10): List<Song> {
       return songs.chunked(batchSize).flatMap { batch ->
           coroutineScope {
               batch.map { song ->
                   async {
                       youtubeMusicClient.getVideoMetadata(song.id).getOrNull()?.let { metadata ->
                           song.copy(/* merge metadata */)
                       } ?: song
                   }
               }.awaitAll()
           }
       }
   }
   ```

---

## 🐛 Troubleshooting

### Issue: Metadata Not Extracted

**Solution:** Check video description format. Metadata extraction works best with structured descriptions.

```kotlin
// Debug metadata extraction
val metadata = youtubeMusicClient.getVideoMetadata(videoId).getOrNull()
println("Description: ${metadata?.description}")
println("Composer: ${metadata?.composer}")
println("Lyricist: ${metadata?.lyricist}")
```

### Issue: Extras Map Empty

**Solution:** Ensure you're using `enhanceSongsWithMetadata()` or `getEnhancedSongDetails()`.

```kotlin
// ❌ Wrong - basic search doesn't populate extras
val songs = musicRepository.searchSongs("query")

// ✅ Correct - enhance to populate extras
val enriched = musicRepository.enhanceSongsWithMetadata(songs)
```

---

## 📚 API Reference

### YouTubeMusicClient

```kotlin
// Get enhanced song details with dual API approach
suspend fun getEnhancedSongDetails(videoId: String): Result<Song?>

// Get video metadata including rich info
suspend fun getVideoMetadata(videoId: String): Result<VideoMetadata>
```

### MusicRepository

```kotlin
// Enhance songs with metadata from YouTube
suspend fun enhanceSongsWithMetadata(songs: List<Song>): List<Song>
```

### VideoMetadata

```kotlin
data class VideoMetadata(
    val videoId: String,
    val description: String,
    val composer: String?,
    val lyricist: String?,
    val producer: String?,
    val musicLabel: String?,
    val movieName: String?,
    val releaseYear: Int?
)
```

---

**Updated:** December 8, 2025  
**Version:** 1.0  
**Maintainer:** REON Music Development Team
