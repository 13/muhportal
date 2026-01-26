# Flutter Development Guide

Quick start guide for working with the Flutter version of MuhPortal.

## Prerequisites

1. **Install Flutter SDK**
   - Download from: https://flutter.dev/docs/get-started/install
   - Minimum version: Flutter 3.0.0, Dart 3.0.0

2. **Set up your IDE**
   - VS Code with Flutter extension, or
   - Android Studio with Flutter plugin, or
   - IntelliJ IDEA with Flutter plugin

3. **Verify installation**
   ```bash
   flutter doctor
   ```

## Quick Start

1. **Get dependencies**
   ```bash
   flutter pub get
   ```

2. **Run the app**
   ```bash
   # List available devices
   flutter devices
   
   # Run on web
   flutter run -d chrome
   
   # Run on Android emulator/device
   flutter run -d android
   
   # Run on iOS simulator/device (macOS only)
   flutter run -d ios
   ```

3. **Run tests**
   ```bash
   flutter test
   ```

## Project Structure

```
lib/
├── main.dart              # App entry point and main screen
├── models.dart            # Data models (PortalUpdate, WolUpdate, etc.)
├── mqtt_client.dart       # MQTT WebSocket client
├── theme.dart             # App theme configuration
├── common_ui.dart         # Reusable UI components
├── portal_screen.dart     # Portal monitoring screen
├── wol_screen.dart        # Wake-on-LAN screen
├── ha_screen.dart         # Home Automation screen
└── settings_screen.dart   # Settings screen

test/
├── model_test.dart        # Model unit tests
└── widget_test.dart       # Widget tests

web/
├── index.html            # Web app entry point
└── manifest.json         # PWA manifest
```

## Development Tips

### Hot Reload
While running the app, press:
- `r` for hot reload (preserves state)
- `R` for hot restart (resets state)
- `q` to quit

### Debugging
```bash
# Run with verbose logging
flutter run -v

# Run in debug mode (default)
flutter run

# Run in profile mode (for performance testing)
flutter run --profile

# Run in release mode
flutter run --release
```

### Code Formatting
```bash
# Format all Dart files
dart format .

# Check formatting without changing files
dart format --set-exit-if-changed .
```

### Code Analysis
```bash
# Run static analysis
flutter analyze
```

## Building for Different Platforms

### Web
```bash
# Development build
flutter build web

# Production build with optimizations
flutter build web --release

# Output location: build/web/
```

### Android
```bash
# Debug APK
flutter build apk --debug

# Release APK
flutter build apk --release

# App Bundle (for Google Play)
flutter build appbundle --release

# Output location: build/app/outputs/
```

### iOS (macOS only)
```bash
# Release build
flutter build ios --release

# Open in Xcode for signing and deployment
open ios/Runner.xcworkspace
```

### Desktop

**Windows:**
```bash
flutter build windows
# Output: build/windows/runner/Release/
```

**macOS:**
```bash
flutter build macos
# Output: build/macos/Build/Products/Release/
```

**Linux:**
```bash
flutter build linux
# Output: build/linux/x64/release/bundle/
```

## Configuration

### MQTT Broker
The MQTT broker address is currently hardcoded in `lib/mqtt_client.dart`:
```dart
static const String serverUri = '192.168.22.5';
static const int serverPort = 1884;
```

To change it, edit these values or implement a configuration screen.

### App Metadata
Edit `pubspec.yaml` to change:
- App name: `name: muhportal`
- Version: `version: 2.4.0+6`
- Description: `description: ...`

## Common Issues

### 1. MQTT Connection Fails
- Ensure the broker is accessible from your device/browser
- Check CORS settings for web deployment
- Verify WebSocket support on the broker

### 2. Hot Reload Not Working
- Try hot restart instead: press `R`
- Restart the development server: `flutter run`

### 3. Web Build CORS Issues
When running locally, you may need to disable web security:
```bash
# Chrome (not recommended for production)
flutter run -d chrome --web-browser-flag "--disable-web-security"
```

### 4. Dependencies Not Found
```bash
# Clean and reinstall
flutter clean
flutter pub get
```

## Testing

### Run All Tests
```bash
flutter test
```

### Run Specific Test File
```bash
flutter test test/model_test.dart
```

### Run Tests with Coverage
```bash
flutter test --coverage
```

### Widget Testing
Use `WidgetTester` for UI tests:
```dart
testWidgets('Portal screen displays correctly', (WidgetTester tester) async {
  await tester.pumpWidget(const MyApp());
  expect(find.text('Portal'), findsOneWidget);
});
```

## Deployment

### Web
1. Build: `flutter build web --release`
2. Deploy `build/web/` to any static hosting (GitHub Pages, Netlify, Firebase Hosting, etc.)

### Android
1. Build: `flutter build appbundle --release`
2. Sign the bundle with your keystore
3. Upload to Google Play Console

### iOS
1. Build: `flutter build ios --release`
2. Open in Xcode: `open ios/Runner.xcworkspace`
3. Archive and upload to App Store Connect

## Performance Optimization

### Reduce App Size
```bash
# Split APKs by ABI
flutter build apk --split-per-abi

# Tree shaking (automatic in release mode)
flutter build web --release
```

### Improve Load Time
- Use lazy loading for screens
- Optimize images in `assets/`
- Enable deferred loading for large dependencies

## Contributing

When contributing to the Flutter version:
1. Follow Dart style guide
2. Run `dart format .` before committing
3. Ensure `flutter analyze` passes
4. Add tests for new features
5. Update documentation

## Resources

- [Flutter Documentation](https://flutter.dev/docs)
- [Dart Language Tour](https://dart.dev/guides/language/language-tour)
- [Flutter Cookbook](https://flutter.dev/docs/cookbook)
- [MQTT Client Package](https://pub.dev/packages/mqtt_client)
- [Material Design 3](https://m3.material.io/)
