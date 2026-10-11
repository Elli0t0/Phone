# Phone

An open-source Android dialer and contacts application with a visual style inspired by Samsung One UI. Built purely in Kotlin using modern Jetpack Compose.

## Project Status

**Version**: `0.1.0` (Project Foundation)

This version establishes the application architecture, Gradle configuration, One UI design system tokens, bottom bar navigation, and initial feature structure. Real contacts querying, call log reading, and telephony features are scheduled for subsequent milestone releases.

## Minimum Android Version

- **Minimum SDK**: Android 10 (API level 29)
- **Target SDK**: Android 15 (API level 35)
- **Compile SDK**: Android 15 (API level 35)

## Architecture & Technology Stack

- **UI**: Jetpack Compose, Material 3 with custom One UI theming
- **Language**: Kotlin 2.2.x (100% Kotlin)
- **Architecture**: MVVM + Repository pattern with Unidirectional Data Flow (`StateFlow`)
- **Dependency Injection**: Manual dependency injection via `AppContainer` in `PhoneApplication`
- **Navigation**: `androidx.navigation.compose` with sealed `Screen` classes

## Build Instructions

Ensure you have JDK 17 installed and configured.

> **Note on Gradle Wrapper**: The Gradle wrapper executable scripts (`gradlew`, `gradlew.bat`) and wrapper jar are not committed to the repository yet. For local builds, install **Gradle 9.3.1** on your system and run `gradle`, or generate the wrapper locally with `gradle wrapper --gradle-version 9.3.1 --distribution-type bin`.

### Run Unit Tests
```bash
gradle testDebugUnitTest
```

### Build Debug APK
```bash
gradle assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/Phone-0.1.0-debug.apk`

### Build Release APK
To build a release-signed APK locally, set the 4 signing environment variables:
```bash
export PHONE_KEYSTORE_PATH="/path/to/release.jks"
export PHONE_KEYSTORE_PASSWORD="your-keystore-password"
export PHONE_KEY_ALIAS="phone-release"
export PHONE_KEY_PASSWORD="your-key-password"

gradle assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/Phone-0.1.0-release.apk`

*Note*: If the signing environment variables are omitted, Gradle produces an unsigned release build.

## GitHub CI & Required Secrets

The automated CI workflow (`.github/workflows/android-build.yml`) compiles, tests, verifies signatures, and publishes release APKs.

To enable release signing in GitHub Actions, configure exactly **4 repository secrets** in **Settings > Secrets and variables > Actions**:
1. `PHONE_KEYSTORE_BASE64`: The release keystore file encoded in Base64 (`base64 -w0 phone-release.jks`).
2. `PHONE_KEYSTORE_PASSWORD`: Keystore store password.
3. `PHONE_KEY_ALIAS`: Keystore entry alias (e.g. `phone-release`).
4. `PHONE_KEY_PASSWORD`: Key password.

## How to Download the APK

Release APKs are published under **GitHub Releases** and available as **Workflow Artifacts**:

1. **GitHub Releases (Recommended)**:
   - Navigate to [https://github.com/Elli0t0/Phone/releases](https://github.com/Elli0t0/Phone/releases).
   - Find the latest release (e.g. `v0.1.0-100`, marked as pre-release for `0.x.x`).
   - Download `Phone-0.1.0.apk` and its checksum `Phone-0.1.0.apk.sha256`.

2. **Workflow Artifacts**:
   - Navigate to [https://github.com/Elli0t0/Phone/actions](https://github.com/Elli0t0/Phone/actions).
   - Select the latest run of the **Android Build** workflow.
   - Scroll to the **Artifacts** section at the bottom.
   - Download the `Phone-0.1.0` artifact zip containing the release APK.

## Permissions (Placeholder)

Version 0.1.0 requires zero runtime permissions. Future versions will declare and request runtime permissions strictly via `ActivityResultContracts.RequestPermission`:
- `android.permission.READ_CONTACTS` / `WRITE_CONTACTS` (Contacts tab)
- `android.permission.READ_CALL_LOG` / `WRITE_CALL_LOG` (Recents tab)
- `android.permission.CALL_PHONE` (Keypad dialing)

## Known Limitations

- **Dialer & Calling**: Dial pad buttons and outgoing call actions are placeholders ("Coming in a later version").
- **Contacts**: Device contacts contract is not queried in v0.1.0.
- **Call History**: Call log queries are not performed in v0.1.0.
