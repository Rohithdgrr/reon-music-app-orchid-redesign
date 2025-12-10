/**
 * UPDATED getStreamUrl METHOD FOR YouTubeMusicClient.kt
 * Replace lines 262-348 with this code
 */

/**
 * Get stream URL for a video ID
 */
suspend fun getStreamUrl(videoId: String): Result<String?> = safeApiCall {
    android.util.Log.d(\"YouTubeMusicClient\", \"Getting stream URL for video: $videoId\")
    
    // Calculate current signature timestamp
    val signatureTimestamp = (System.currentTimeMillis() / 1000 / 86400).toInt()
    
    var audioUrl: String? = null
    
    // Try Android Music client first (best for audio)
    try {
        android.util.Log.d(\"YouTubeMusicClient\", \"  Trying ANDROID_MUSIC client...\")
        val androidRequestBody = buildJsonObject {
            put(\"videoId\", videoId)
            put(\"context\", buildJsonObject {
                put(\"client\", buildJsonObject {
                    put(\"clientName\", \"ANDROID_MUSIC\")
                    put(\"clientVersion\", \"7.02.52\")
                    put(\"androidSdkVersion\", 34)
                    put(\"hl\", \"en\")
                    put(\"gl\", \"US\")
                    put(\"utcOffsetMinutes\", 330)
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
        
        val response: HttpResponse = httpClient.post(\"$INNERTUBE_API_URL/player?key=$API_KEY\") {
            contentType(ContentType.Application.Json)
            header(\"User-Agent\", \"com.google.android.apps.youtube.music/7.02.52 (Linux; U; Android 14; en_US) gzip\")
            header(\"X-Youtube-Client-Name\", \"21\")
            header(\"X-Youtube-Client-Version\", \"7.02.52\")
            header(\"Accept-Language\", \"en-US,en;q=0.9\")
            setBody(androidRequestBody.toString())
        }
        
        val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        audioUrl = extractAudioUrl(responseJson)
        
        if (audioUrl != null) {
            android.util.Log.d(\"YouTubeMusicClient\", \"  ✓ ANDROID_MUSIC client success!\")
        } else {
            android.util.Log.w(\"YouTubeMusicClient\", \"  ANDROID_MUSIC client returned no URL\")
        }
    } catch (e: Exception) {
        android.util.Log.e(\"YouTubeMusicClient\", \"  ANDROID_MUSIC client error: ${e.message}\", e)
    }
    
    // If Android didn't work, try iOS Music client
    if (audioUrl == null) {
        try {
            android.util.Log.d(\"YouTubeMusicClient\", \"  Trying IOS_MUSIC client...\")
            val iosRequestBody = buildJsonObject {
                put(\"videoId\", videoId)
                put(\"context\", buildJsonObject {
                    put(\"client\", buildJsonObject {
                        put(\"clientName\", \"IOS_MUSIC\")
                        put(\"clientVersion\", \"7.02\")
                        put(\"deviceModel\", \"iPhone16,2\")
                        put(\"osVersion\", \"18.0.0.22A3351\")
                        put(\"hl\", \"en\")
                        put(\"gl\", \"US\")
                        put(\"utcOffsetMinutes\", 330)
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
            
            val response: HttpResponse = httpClient.post(\"$INNERTUBE_API_URL/player?key=$API_KEY\") {
                contentType(ContentType.Application.Json)
                header(\"User-Agent\", \"com.google.ios.youtube.music/7.02 (iPhone16,2; U; CPU iOS 18_0 like Mac OS X; en_US)\")
                header(\"X-Youtube-Client-Name\", \"26\")
                header(\"X-Youtube-Client-Version\", \"7.02\")
                header(\"Accept-Language\", \"en-US,en;q=0.9\")
                setBody(iosRequestBody.toString())
            }
            
            val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            audioUrl = extractAudioUrl(responseJson)
            
            if (audioUrl != null) {
                android.util.Log.d(\"YouTubeMusicClient\", \"  ✓ IOS_MUSIC client success!\")
            } else {
                android.util.Log.w(\"YouTubeMusicClient\", \"  IOS_MUSIC client returned no URL\")
            }
        } catch (e: Exception) {
            android.util.Log.e(\"YouTubeMusicClient\", \"  IOS_MUSIC client error: ${e.message}\", e)
        }
    }
    
    // Try WEB client as last resort
    if (audioUrl == null) {
        try {
            android.util.Log.d(\"YouTubeMusicClient\", \"  Trying WEB client as fallback...\")
            val webRequestBody = buildJsonObject {
                put(\"videoId\", videoId)
                put(\"context\", CLIENT_CONTEXT)
                put(\"playbackContext\", buildJsonObject {
                    put(\"contentPlaybackContext\", buildJsonObject {
                        put(\"signatureTimestamp\", signatureTimestamp)
                    })
                })
                put(\"racyCheckOk\", true)
                put(\"contentCheckOk\", true)
            }
            
            val response: HttpResponse = httpClient.post(\"$INNERTUBE_API_URL/player?key=$API_KEY\") {
                contentType(ContentType.Application.Json)
                header(\"User-Agent\", USER_AGENT)
                header(\"Origin\", \"https://music.youtube.com\")
                header(\"Referer\", \"https://music.youtube.com/\")
                setBody(webRequestBody.toString())
            }
            
            val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            audioUrl = extractAudioUrl(responseJson)
            
            if (audioUrl != null) {
                android.util.Log.d(\"YouTubeMusicClient\", \"  ✓ WEB client success!\")
            } else {
                android.util.Log.w(\"YouTubeMusicClient\", \"  WEB client returned no URL\")
            }
        } catch (e: Exception) {
            android.util.Log.e(\"YouTubeMusicClient\", \"  WEB client error: ${e.message}\", e)
        }
    }
    
    if (audioUrl == null) {
        android.util.Log.e(\"YouTubeMusicClient\", \"✗ All clients failed for video: $videoId\")
    }
    
    audioUrl
}
