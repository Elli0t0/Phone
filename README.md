# Phone

An open-source Android dialer and contacts application with a visual style inspired by Samsung One UI. Built purely in Kotlin using modern Jetpack Compose.

## Project Status

**Version**: `0.1.3` (Dial Pad)

- **Foundation (0.1.0)**: Architecture, Gradle build system, One UI design system tokens, bottom navigation.
- **Contacts (0.1.2)**: Real device contacts management via `ContactsContract` and `ContentResolver`. Reactive flows via `ContentObserver`, One UI contact list with search (name & digits-insensitive), section headers, contact details, differential in-place editing, creation, and deletion.
- **Dial Pad (0.1.3)**: Samsung One UI interactive dialer, circular keys (`1-9`, `*`, `0`, `#`) with letter sublabels, T9 search-as-you-type with 150 ms debounce, contact search results with tap-to-dial and direct call buttons, non-mutating display formatting (`PhoneNumberFormatter`), long-press `0` for `+`, accelerated repeating backspace without skipping, call placement via `TelecomManager.placeCall` with `ACTION_DIAL` fallback, safe USSD (`*#`) URL encoding, haptics, and rotation-proof state.

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
- **Contacts Data Layer**: `ContactsContract` ContentProvider with batch `ContentProviderOperation`

## Build Instructions

Ensure you have JDK 17 installed and configured.

> **Note on Gradle Wrapper**: The Gradle wrapper executable scripts (`gradlew`, `gradlew.bat`) and wrapper jar are not committed to the repository. For local builds, install **Gradle 9.3.1** on your system and run `gradle`, or generate the wrapper locally with `gradle wrapper --gradle-version 9.3.1 --distribution-type bin`.

### Run Unit Tests
```bash
gradle testDebugUnitTest
```

### Build Debug APK
```bash
gradle assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/Phone-0.1.2-debug.apk`

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
`app/build/outputs/apk/release/Phone-0.1.2-release.apk`

*Note*: If the signing environment variables are omitted, Gradle produces an unsigned release build.

## GitHub CI & Required Secrets

The automated CI workflow (`.github/workflows/android-build.yml`) compiles, tests, verifies signatures, and publishes release APKs to GitHub Releases.

To enable release signing in GitHub Actions, configure exactly **4 repository secrets** in **Settings > Secrets and variables > Actions**:
1. `PHONE_KEYSTORE_BASE64`: The release keystore file encoded in Base64 (`base64 -w0 phone-release.jks`).
2. `PHONE_KEYSTORE_PASSWORD`: Keystore store password.
3. `PHONE_KEY_ALIAS`: Keystore entry alias (e.g. `phone-release`).
4. `PHONE_KEY_PASSWORD`: Key password.

## How to Download the APK

Release APKs are published under **GitHub Releases** and available as **Workflow Artifacts**:

1. **GitHub Releases (Recommended)**:
   - Navigate to [https://github.com/Elli0t0/Phone/releases](https://github.com/Elli0t0/Phone/releases).
   - Find the latest release (e.g. `v0.1.2-102`, marked as pre-release for `0.x.x`).
   - Download `Phone-0.1.2.apk` and its checksum `Phone-0.1.2.apk.sha256`.

2. **Workflow Artifacts**:
   - Navigate to [https://github.com/Elli0t0/Phone/actions](https://github.com/Elli0t0/Phone/actions).
   - Select the latest run of the **Android Build** workflow.
   - Scroll to the **Artifacts** section at the bottom.
   - Download the `Phone-0.1.2` artifact zip containing the release APK.

## Permissions

- `android.permission.READ_CONTACTS`: Display, search, and view device contacts.
- `android.permission.WRITE_CONTACTS`: Create, edit, and delete contacts on device.

Handled gracefully at runtime with comprehensive state representation (Not Asked, Denied, Permanently Denied with App Settings button, Granted).

## Known Limitations

- **Dialer & Calling**: Dial pad buttons and outgoing call actions are placeholders ("Coming in a later version").
- **Call History**: Call log queries are scheduled for an upcoming milestone release.
