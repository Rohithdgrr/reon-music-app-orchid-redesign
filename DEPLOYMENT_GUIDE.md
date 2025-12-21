# REON Music App - Deployment Guide

This guide will help you build and deploy the REON Music app.

## Prerequisites

1. **Android Studio** (latest version recommended)
2. **JDK 17** or higher
3. **Android SDK** with API level 34
4. **Gradle** (included via gradlew)

## Building the Application

### Option 1: Using PowerShell Script (Windows)

```powershell
cd REONm
.\deploy.ps1
```

### Option 2: Using Gradle Commands

#### Build Debug APK (for testing)
```bash
./gradlew assembleFullDebug
# or for FOSS version
./gradlew assembleFossDebug
```

#### Build Release APK (unsigned)
```bash
./gradlew assembleFullRelease
# or for FOSS version
./gradlew assembleFossRelease
```

#### Build Release AAB (for Google Play Store)
```bash
./gradlew bundleFullRelease
```

### Option 3: Using Android Studio

1. Open the project in Android Studio
2. Go to **Build** → **Generate Signed Bundle / APK**
3. Select **Android App Bundle** or **APK**
4. Choose your keystore (or create a new one)
5. Select **release** build variant
6. Click **Finish**

## App Variants

The app has two variants:

### 1. FOSS Version (`foss`)
- **Package ID**: `com.reon.music.foss`
- **Features**: No crash analytics, no tracking
- **Use Case**: For users who prefer completely open-source apps

### 2. Full Version (`full`)
- **Package ID**: `com.reon.music`
- **Features**: Optional crash reporting (Sentry), no tracking
- **Use Case**: Standard release with optional analytics

## Signing the App

### Creating a Keystore (First Time)

```bash
keytool -genkey -v -keystore reon-music-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias reon-music-key
```

You'll be prompted for:
- Keystore password
- Key password
- Your name and organization details

### Configuring Signing in build.gradle.kts

1. Create a `keystore.properties` file in the project root:
```properties
storeFile=../reon-music-keystore.jks
storePassword=your_keystore_password
keyAlias=reon-music-key
keyPassword=your_key_password
```

2. Update `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = Properties()
        if (keystorePropertiesFile.exists()) {
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}

buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        signingConfig = signingConfigs.getByName("release")
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

## Output Locations

After building, you'll find the output files at:

- **Debug APK**: `app/build/outputs/apk/full/debug/app-full-debug.apk`
- **Release APK**: `app/build/outputs/apk/full/release/app-full-release.apk`
- **FOSS Release APK**: `app/build/outputs/apk/foss/release/app-foss-release.apk`
- **Release AAB**: `app/build/outputs/bundle/fullRelease/app-full-release.aab`

## Testing Before Deployment

1. **Install on Device**:
   ```bash
   adb install app/build/outputs/apk/full/release/app-full-release.apk
   ```

2. **Test All Features**:
   - Music playback
   - Search functionality
   - Playlist creation
   - Downloads
   - Radio mode
   - Sorting features

## Deploying to Google Play Store

1. **Create a Google Play Console Account** (if you don't have one)
2. **Create a New App** in Play Console
3. **Upload the AAB file** (not APK) to the Play Console
4. **Fill in Store Listing**:
   - App name: REON Music
   - Short description
   - Full description
   - Screenshots
   - App icon
   - Feature graphic
5. **Set Content Rating**
6. **Complete Privacy Policy** (required)
7. **Submit for Review**

## Deploying to Other Stores

### F-Droid
- Build the FOSS variant
- Follow F-Droid submission guidelines
- Provide source code repository

### Direct Distribution
- Build signed release APK
- Host on your website
- Provide download link

## Version Management

Update version in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2  // Increment for each release
    versionName = "1.0.1"  // Semantic versioning
}
```

## ProGuard/R8 Configuration

The app uses ProGuard/R8 for code shrinking. Rules are in:
- `app/proguard-rules.pro`

Make sure to test the release build thoroughly as ProGuard might remove necessary code.

## Troubleshooting

### Build Fails
1. Clean build: `./gradlew clean`
2. Invalidate caches in Android Studio
3. Check for dependency conflicts

### APK Too Large
- Enable ProGuard/R8 (already enabled)
- Use Android App Bundle (AAB) instead of APK
- Remove unused resources

### Signing Issues
- Verify keystore file exists
- Check keystore.properties file
- Ensure passwords are correct

## Security Notes

⚠️ **Important**: Never commit `keystore.properties` or `.jks` files to version control!

Add to `.gitignore`:
```
keystore.properties
*.jks
*.keystore
```

## Support

For issues or questions:
- Check the README.md
- Review ARCHITECTURE.md
- Check build logs in the project root

---

**Good luck with your deployment! 🚀**

