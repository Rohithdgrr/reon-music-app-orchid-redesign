# REON Music - Streaming Fix Guide

## Issue Identified

The app is unable to stream music because the YouTube stream URL resolution is failing. Songs show 0:00 duration and won't play.

## Root Causes

1. **Outdated YouTube API Client Information** - Client versions and signatures need updating
2. **Insufficient Logging** - Hard to diagnose exact failure points
3. **Piped Instance Availability** - Some Piped instances may be down
4. **Network/Timeout Issues** - Missing timeout configurations

## Fixes Applied

### 1. Enhanced StreamResolver (`data/network/src/main/java/com/reon/music/data/network/StreamResolver.kt`)

**Changes Made:**
- ✅ Added comprehensive logging at each resolution step
- ✅ Updated Piped instance list with working alternatives
- ✅ Added timeout configurations (3-5 seconds per request)
- ✅ Improved error messages to identify exact failure points
- ✅ Reordered resolution attempts (InnerTube → Piped → Alternatives)

**New Logging Format:**
```
============================================
Resolving stream for: [Song Title] by [Artist]
Video ID: [videoId]
Source: youtube
============================================
[Method 1/3] Trying InnerTube API for: [videoId]
✓ InnerTube API success! URL length: [length]
✓ Successfully resolved stream URL
```

### 2. YouTube Music Client Updates Needed

**File:** `data/network/src/main/java/com/reon/music/data/network/youtube/YouTubeMusicClient.kt`

**Manual Updates Required** (lines 262-348):

Replace the `getStreamUrl` method with updated client data:

```kotlin
suspend fun getStreamUrl(videoId: String): Result<String?> = safeApiCall {
    android.util.Log.d(\"YouTubeMusicClient\", \"Getting stream URL for video: $videoId\")
    
    val signatureTimestamp = (System.currentTimeMillis() / 1000 / 86400).toInt()
    var audioUrl: String? = null
    
    // Try ANDROID_MUSIC client (updated version)
    try {
        android.util.Log.d(\"YouTubeMusicClient\", \"  Trying ANDROID_MUSIC client...\")
        val androidRequestBody = buildJsonObject {
            put(\"videoId\", videoId)
            put(\"context\", buildJsonObject {
                put(\"client\", buildJsonObject {
                    put(\"clientName\", \"ANDROID_MUSIC\")
                    put(\"clientVersion\", \"7.02.52\")  // UPDATED
                    put(\"androidSdkVersion\", 34)       // UPDATED
                    put(\"hl\", \"en\")
                    put(\"gl\", \"US\")
                })
            })
            put(\"playbackContext\", buildJsonObject {
                put(\"contentPlaybackContext\", buildJsonObject {
                    put(\"signatureTimestamp\", signatureTimestamp)
                })
            })
            put(\"racyCheckOk\", true)
            put(\"contentCheckOk\", true)
        }
        
        val response = httpClient.post(\"$INNERTUBE_API_URL/player?key=$API_KEY\") {
            contentType(ContentType.Application.Json)
            header(\"User-Agent\", \"com.google.android.apps.youtube.music/7.02.52 (Linux; U; Android 14; en_US) gzip\")
            header(\"X-Youtube-Client-Name\", \"21\")
            header(\"X-Youtube-Client-Version\", \"7.02.52\")
            setBody(androidRequestBody.toString())
        }
        
        audioUrl = extractAudioUrl(Json.parseToJsonElement(response.bodyAsText()).jsonObject)
        
        if (audioUrl != null) {
            android.util.Log.d(\"YouTubeMusicClient\", \"  ✓ ANDROID_MUSIC client success!\")
        }
    } catch (e: Exception) {
        android.util.Log.e(\"YouTubeMusicClient\", \"  ANDROID_MUSIC error: ${e.message}\", e)
    }
    
    // Try IOS_MUSIC client if Android failed
    if (audioUrl == null) {
        try {
            // Similar updates for iOS client...
            android.util.Log.d(\"YouTubeMusicClient\", \"  Trying IOS_MUSIC client...\")
            // ... iOS implementation
        } catch (e: Exception) {
            android.util.Log.e(\"YouTubeMusicClient\", \"  IOS_MUSIC error: ${e.message}\", e)
        }
    }
    
    audioUrl
}
```

## Updated Piped Instances

The following Piped instances are now tried in order:
1. `https://api.piped.projectsegfau.lt`
2. `https://pipedapi.kavin.rocks`
3. `https://api.piped.privacydev.net`
4. `https://pipedapi.adminforge.de`
5. `https://piped-api.hostux.net`

## Testing the Fix

### Step 1: Rebuild the App

```bash
cd "C:\Users\rohit\Music\REON LIBRA app using flutter first edition\project app 1\REONm"
.\gradlew.bat clean
.\gradlew.bat assembleFullDebug
```

### Step 2: Install on Device

```bash
# If you have adb configured:
adb install -r app\build\outputs\apk\full\debug\app-full-debug.apk

# Or copy the APK to your phone and install manually
```

### Step 3: Check Logs

After installing, when you try to play a song, check the logs:

```bash
adb logcat | findstr "StreamResolver\|YouTubeMusicClient"
```

**Expected Log Output (Success):**
```
D StreamResolver: ============================================
D StreamResolver: Resolving stream for: Chikiri Chikiri by Mohit Chauhan
D StreamResolver: Video ID: xyz123
D StreamResolver: Source: youtube
D StreamResolver: ============================================
D StreamResolver: [Method 1/3] Trying InnerTube API for: xyz123
D YouTubeMusicClient: Getting stream URL for video: xyz123
D YouTubeMusicClient:   Trying ANDROID_MUSIC client...
D YouTubeMusicClient:   ✓ ANDROID_MUSIC client success!
D StreamResolver: ✓ Successfully resolved stream URL (1234 chars)
```

**Error Log Output (Failure):**
```
D StreamResolver: [Method 1/3] Trying InnerTube API for: xyz123
E YouTubeMusicClient:   ANDROID_MUSIC error: HTTP 403
D StreamResolver: [Method 2/3] Trying Piped API for: xyz123
D StreamResolver: [Method 3/3] Trying alternative Piped instances
E StreamResolver: ✗ All stream resolution methods failed
```

## Alternative Quick Fix (If Build Fails)

If you're having trouble building, you can use this quick workaround:

### Option 1: Use Different Piped Instance Manually

Edit `StreamResolver.kt` and move the most reliable instance to the top:

```kotlin
val alternativeSources = listOf(
    \"https://pipedapi.kavin.rocks\",  // Usually most reliable
    \"https://api.piped.projectsegfau.lt\",
    // ... others
)
```

### Option 2: Simplified Build Command

From the project directory, run:

```powershell
cd "C:\Users\rohit\Music\REON LIBRA app using flutter first edition\project app 1\REONm"

# Clean build artifacts
Remove-Item -Recurse -Force .\\.gradle, .\\app\\build, .\\data\\network\\build -ErrorAction SilentlyContinue

# Build
.\\gradlew clean assembleFullDebug
```

## Common Issues & Solutions

### Issue 1: "All stream resolution methods failed"
**Solution:** 
- Check internet connection
- Try different Piped instance
- Update YouTube client versions (see manual update above)

### Issue 2: "403 Forbidden" errors
**Solution:**
- YouTube may have blocked the client
- Update client versions in YouTubeMusicClient.kt
- Use VPN if region-blocked

### Issue 3: Songs load but show 0:00 duration
**Solution:**
- This means stream URL is not being resolved
- Check logs to see which method is failing
- Ensure StreamResolver.kt was updated correctly

### Issue 4: Build errors after changes
**Solution:**
```bash
# Clean everything
.\\gradlew clean

# Delete build folders manually
Remove-Item -Recurse -Force .gradle, build, app/build, data/network/build

# Rebuild
.\\gradlew assembleFullDebug
```

## Files Modified

1. ✅ **StreamResolver.kt** - Enhanced with logging and better fallbacks
2. ⏳ **YouTubeMusicClient.kt** - Needs manual update (see section above)

## Next Steps

1. **Update YouTubeMusicClient.kt** manually (copy the code from section 2)
2. **Rebuild the app**
3. **Test streaming** on a few songs
4. **Check logs** if streaming still fails
5. **Report back** with log output for further debugging

## Additional Resources

- YouTube InnerTube API Documentation: https://github.com/TeamNewPipe/NewPipeExtractor
- Piped API Documentation: https://github.com/Team Piped/Piped
- Debugging Android Apps: Use `adb logcat` to view real-time logs

---

**Status:** StreamResolver updated ✅ | YouTubeMusicClient needs manual update ⏳

**Created:** December 8, 2025  
**Last Updated:** December 8, 2025 21:40 IST
