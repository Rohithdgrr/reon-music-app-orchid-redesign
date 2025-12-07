# Keystore Setup Guide

## Quick Setup (For Testing - Unsigned Build)

The app can be built without signing for testing purposes. However, for production deployment, you need a signed build.

## Creating a Keystore for Production

### Step 1: Generate Keystore

Open PowerShell or Command Prompt in the REONm directory and run:

```bash
keytool -genkey -v -keystore reon-music-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias reon-music-key
```

You'll be asked for:
- **Keystore password**: (Remember this!)
- **Re-enter password**: (Same password)
- **First and last name**: Your name or organization
- **Organizational unit**: (Optional)
- **Organization**: Your organization name
- **City**: Your city
- **State**: Your state/province
- **Country code**: Two-letter code (e.g., US, IN)

### Step 2: Create keystore.properties

Create a file named `keystore.properties` in the REONm directory:

```properties
storeFile=reon-music-keystore.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=reon-music-key
keyPassword=YOUR_KEY_PASSWORD
```

**⚠️ IMPORTANT**: 
- Replace `YOUR_KEYSTORE_PASSWORD` and `YOUR_KEY_PASSWORD` with your actual passwords
- **DO NOT** commit this file to Git!
- Add `keystore.properties` and `*.jks` to `.gitignore`

### Step 3: Update build.gradle.kts

The signing configuration is already set up in `app/build.gradle.kts`. Just uncomment and update if needed:

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
```

And in `buildTypes`:
```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.getByName("release")
    // ... rest of config
}
```

### Step 4: Build Signed Release

After setting up the keystore, build the signed release:

```bash
gradlew.bat assembleFullRelease
```

The signed APK will be at:
`app/build/outputs/apk/full/release/app-full-release.apk`

## Backup Your Keystore!

⚠️ **CRITICAL**: Keep your keystore file and passwords safe! If you lose them:
- You cannot update your app on Google Play Store
- You'll need to create a new app listing
- Users will need to uninstall and reinstall

**Backup locations:**
- Store keystore file in secure cloud storage
- Keep passwords in a password manager
- Document the keystore location

## For Testing (Unsigned Build)

If you just want to test the app, you can build an unsigned release:

```bash
gradlew.bat assembleFullRelease
```

This will create an unsigned APK that can be installed for testing, but cannot be published to app stores.

