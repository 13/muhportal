# Flutter Migration Guide

This document explains the migration from the original Android/Kotlin implementation to the new Flutter cross-platform implementation.

## Overview

MuhPortal has been successfully ported from a Kotlin/Android application to a Flutter application, enabling cross-platform support for:
- Web browsers
- Android devices
- iOS devices
- Desktop (Windows, macOS, Linux)

## Architecture Comparison

### Original Android/Kotlin Implementation
- **Location**: `app/` directory
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **MQTT Library**: Eclipse Paho MQTT Client
- **Build System**: Gradle
- **Platform**: Android only

### New Flutter Implementation
- **Location**: `lib/` directory
- **Language**: Dart
- **UI Framework**: Flutter
- **MQTT Library**: mqtt_client (Dart)
- **Build System**: Flutter/Dart build system
- **Platforms**: Web, Android, iOS, Windows, macOS, Linux

## File Mapping

| Android/Kotlin | Flutter/Dart | Description |
|----------------|--------------|-------------|
| `app/src/main/java/.../MainActivity.kt` | `lib/main.dart` | Main app entry point |
| `app/src/main/java/.../PortalMqttClient.kt` | `lib/mqtt_client.dart` | MQTT client implementation |
| `app/src/main/java/.../PortalScreen.kt` | `lib/portal_screen.dart` | Portal monitoring UI |
| `app/src/main/java/.../WolScreen.kt` | `lib/wol_screen.dart` | Wake-on-LAN UI |
| `app/src/main/java/.../HAScreen.kt` | `lib/ha_screen.dart` | Home Automation UI |
| `app/src/main/java/.../SettingsScreen.kt` | `lib/settings_screen.dart` | Settings UI |
| `app/src/main/java/.../CommonUi.kt` | `lib/common_ui.dart` | Shared UI components |
| `app/src/main/java/.../ui/theme/` | `lib/theme.dart` | Theme configuration |
| Data classes in `PortalMqttClient.kt` | `lib/models.dart` | Data models |

## Key Differences

### 1. MQTT Client
- **Android**: Uses Eclipse Paho with Android Service
- **Flutter**: Uses `mqtt_client` package with browser WebSocket support for web

### 2. State Management
- **Android**: Compose state with `remember`, `mutableStateOf`
- **Flutter**: StatefulWidget with `setState()`

### 3. Navigation
- **Android**: HorizontalPager with NavigationBar
- **Flutter**: PageView equivalent with BottomNavigationBar

### 4. Persistent Storage
- **Android**: SharedPreferences (Android API)
- **Flutter**: `shared_preferences` package (cross-platform)

### 5. UI Components
- **Android**: Material 3 Compose components
- **Flutter**: Material 3 Flutter widgets

## Running the Applications

### Run Flutter Version (Recommended)
```bash
# Web
flutter run -d chrome

# Android
flutter run -d android

# iOS
flutter run -d ios

# Desktop
flutter run -d windows
```

### Run Android/Kotlin Version
```bash
# From Android Studio or
./gradlew assembleDebug
```

## Testing

### Flutter Tests
```bash
flutter test
```

### Android Tests
```bash
./gradlew test
```

## Building for Production

### Flutter
```bash
# Web
flutter build web

# Android
flutter build apk --release

# iOS
flutter build ios --release
```

### Android/Kotlin
```bash
./gradlew assembleRelease
```

## Migration Notes

1. **Code Structure**: The Flutter version maintains the same logical structure as the Kotlin version but adapts to Flutter's patterns.

2. **MQTT Protocol**: Both versions use the same MQTT protocol and topics, so they can communicate with the same broker.

3. **Features**: All features from the Android version have been ported to Flutter:
   - Portal monitoring and control
   - Wake-on-LAN functionality
   - Home Automation sensors and switches
   - Settings (dark mode, black & white mode)
   - Offline caching

4. **UI/UX**: The Flutter version maintains visual consistency with the Android version while adapting to platform-specific conventions.

## Maintenance

Both implementations can be maintained in parallel:
- **Android/Kotlin version** (`app/`): For Android-specific features or optimizations
- **Flutter version** (`lib/`): For cross-platform support and new features

For new development, the Flutter version is recommended for its cross-platform capabilities.

## Dependencies

### Flutter (`pubspec.yaml`)
- flutter
- mqtt_client: ^10.2.0
- shared_preferences: ^2.2.2
- intl: ^0.18.1

### Android (`app/build.gradle.kts`)
- androidx.compose.*
- org.eclipse.paho:org.eclipse.paho.client.mqttv3
- org.eclipse.paho:org.eclipse.paho.android.service

## Known Limitations

1. **MQTT Broker**: The hardcoded broker address (`ws://192.168.22.5:1884`) needs to be accessible from all platforms.

2. **Web CORS**: When running the Flutter web version, ensure the MQTT broker accepts WebSocket connections from the web origin.

3. **Platform-Specific Features**: Some Android-specific optimizations may not be available in the Flutter version.

## Future Enhancements

Potential improvements for the Flutter version:
- [ ] Add configuration UI for MQTT broker settings
- [ ] Implement push notifications for portal state changes
- [ ] Add more unit and integration tests
- [ ] Optimize for tablet and desktop layouts
- [ ] Add accessibility features
- [ ] Implement CI/CD for automated builds
