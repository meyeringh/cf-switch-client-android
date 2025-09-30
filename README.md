# CF Switch Android Client

A minimal, modern Android app for controlling [cf-switch](https://github.com/meyeringh/cf-switch) rules.

## Features

- **Simple Toggle Control**: View and toggle rule state with a single tap
- **Pull-to-Refresh**: Swipe down to refresh the current rule state
- **Secure Configuration**: Base URL and API token stored using encrypted shared preferences
- **Material 3 Design**: Modern UI with dynamic color support and dark mode
- **Offline-First**: Optimistic updates with automatic retry on failure
- **Comprehensive Testing**: Unit tests and E2E instrumented tests with MockWebServer

## Requirements

- Android 8.0 (API 26) or higher
- A running cf-switch server instance
- API Bearer token from your cf-switch server

## Setup

### 1. Install the App

Download the latest APK from the [Releases](https://github.com/meyeringh/cf-switch-client-android/releases) page.

### 2. Configure Settings

On first launch, the app will automatically open the Settings sheet. Configure:

- **Base URL**: Your cf-switch server URL (e.g., `https://my-cf-switch.example.com/`)
  - Must end with a trailing slash `/`
  - If running on an emulator against localhost, use `http://10.0.2.2:8080/`
- **API Token**: Your Bearer token from cf-switch

Tap **Save** to apply the configuration.

### 3. Use the App

- The main screen shows the current rule state (**Enabled** or **Disabled**)
- Tap the large button to toggle the rule
- Pull down to refresh the state from the server
- Access Settings anytime via the menu icon (⋮) in the top-right corner

## Development

### Prerequisites

- JDK 17
- Android Studio Ladybug or later
- Android SDK with API 35

### Building Locally

```bash
# Clone the repository
git clone https://github.com/meyeringh/cf-switch-client-android.git
cd cf-switch-client-android

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease
```

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests on a connected device or emulator
./gradlew connectedAndroidTest

# Run instrumented tests on Gradle-managed device
./gradlew pixel6Api34DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect

# Check code formatting
./gradlew spotlessCheck

# Apply code formatting
./gradlew spotlessApply
```

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/dev/meyeringh/cfswitch/
│   │   │   ├── data/
│   │   │   │   └── CfSwitchRepository.kt
│   │   │   ├── network/
│   │   │   │   └── CfSwitchApi.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   ├── CfSwitchScreen.kt
│   │   │   │   └── SettingsSheet.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── CfSwitchViewModel.kt
│   │   │   └── ServiceLocator.kt
│   │   └── AndroidManifest.xml
│   ├── test/
│   │   └── java/dev/meyeringh/cfswitch/
│   │       └── CfSwitchRepositoryTest.kt
│   └── androidTest/
│       └── java/dev/meyeringh/cfswitch/
│           └── CfSwitchE2ETest.kt
└── build.gradle.kts
```

### Architecture

The app uses a simple, pragmatic architecture:

- **ServiceLocator**: Provides singleton instances of dependencies (no DI framework)
- **Repository**: Handles network requests and error translation
- **ViewModel**: Manages UI state and business logic
- **Compose UI**: Material 3 components with accessibility support

## CI/CD

The project uses GitHub Actions for continuous integration and release automation.

### Release Pipeline

When a new GitHub release is created:

1. **Test Job**:
   - Runs Spotless code formatting checks
   - Executes unit tests
   - Runs instrumented E2E tests on a Gradle-managed emulator (Pixel 6, API 34)

2. **Build Job**:
   - Decodes the signing keystore from GitHub secrets
   - Builds a signed release APK
   - Uploads the APK as an asset to the GitHub release

### Required GitHub Secrets

To enable signed release builds, configure these secrets in your repository:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded keystore file
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password
- `ANDROID_KEY_ALIAS`: Key alias
- `ANDROID_KEY_PASSWORD`: Key password

To create the base64-encoded keystore:
```bash
base64 -i your-keystore.jks | tr -d '\n'
```

## API Reference

The app communicates with cf-switch using these endpoints:

- `GET /v1/rule` - Fetch current rule state
  - Response: `{"enabled": true|false}`
- `POST /v1/rule/enable` - Toggle rule state
  - Body: `{"enabled": true|false}`

All requests include the `Authorization: Bearer <token>` header.

### Error Handling

- **401/403**: Shows "Invalid token" error message
- **Network errors**: Shows "Network error" with retry action
- **Timeout configuration**: 5s connect, 10s read

## License

See [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and formatting: `./gradlew test spotlessApply`
5. Submit a pull request

## Support

For issues and questions:
- [GitHub Issues](https://github.com/meyeringh/cf-switch-client-android/issues)
- [cf-switch Server](https://github.com/meyeringh/cf-switch)