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

## Build Commands

Ensure you have JDK 17 installed and configured.

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/Phone-0.1.0.apk`

### Build Release APK
To build a release-signed APK, set the 4 signing environment variables:
```bash
export PHONE_KEYSTORE_PATH="/path/to/release.jks"
export PHONE_KEYSTORE_PASSWORD="your-keystore-password"
export PHONE_KEY_ALIAS="phone-release"
export PHONE_KEY_PASSWORD="your-key-password"

./gradlew assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/Phone-0.1.0.apk`

*Note*: If the signing environment variables are omitted, Gradle produces an unsigned release build.

## How to Download the APK

1. Navigate to the GitHub repository: `https://github.com/elli0t0/phone`
2. Click on the **Actions** tab.
3. Select the latest successful run of the **Android Build** workflow.
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Download the `Phone-0.1.0` artifact zip and extract `Phone-0.1.0.apk`.
6. Transfer the APK to your Android device running Android 10 or later and install.

## Permissions (Placeholder)

Version 0.1.0 requires zero runtime permissions. Future versions will declare and request runtime permissions strictly via `ActivityResultContracts.RequestPermission`:
- `android.permission.READ_CONTACTS` / `WRITE_CONTACTS` (Contacts tab)
- `android.permission.READ_CALL_LOG` / `WRITE_CALL_LOG` (Recents tab)
- `android.permission.CALL_PHONE` (Keypad dialing)

## Known Limitations

- **Dialer & Calling**: Dial pad buttons and outgoing call actions are placeholders ("Coming in a later version").
- **Contacts**: Device contacts contract is not queried in v0.1.0.
- **Call History**: Call log queries are not performed in v0.1.0.
