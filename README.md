# React Native Google Sign-In

[![Dependabot](https://img.shields.io/badge/dependabot-enabled-brightgreen.svg)](https://dependabot.com)
[![Security Status](https://img.shields.io/badge/security-monitored-brightgreen.svg)](https://github.com/DarkSorrow/rn-google-signin/security)
[![Dependencies](https://img.shields.io/badge/dependencies-up%20to%20date-brightgreen.svg)](https://github.com/DarkSorrow/rn-google-signin)

A modern, high-performance React Native Google Sign-In library built exclusively for **Turbo Modules** with automatic configuration detection. Part of the **Novastera** open-source ecosystem - a modern CRM/ERP platform that combines enterprise-grade authentication with comprehensive business management tools.

> **⚠️ Important**: This package requires React Native **New Architecture (TurboModules)** and does **NOT** support the legacy bridge architecture.

## Features

- **🚀 Turbo Modules**: Built with React Native's new architecture for maximum performance
- **🔧 Automatic Configuration**: Auto-detects client IDs from `google-services.json` and `GoogleService-Info.plist`
- **📱 Cross-Platform**: Full support for iOS and Android with platform-optimized implementations
- **🔒 Security First**: Built-in nonce support and secure credential management
- **⚡ Optimized Performance**: Thread-safe operations with intelligent caching and resource management
- **🔌 Expo Ready**: Includes Expo config plugin for seamless integration

## Requirements

- **React Native 0.79+** with New Architecture enabled
- **iOS 15.1+**
- **Android API 24+**
- **Google Play Services** (Android)

## Quick Start

### Installation

```bash
npm install @novastera-oss/rn-google-signin
# or
yarn add @novastera-oss/rn-google-signin
```

### Configuration

Configure the library with your Google client IDs:

```typescript
import RnGoogleSignin from '@novastera-oss/rn-google-signin';

// Manual configuration (recommended)
RnGoogleSignin.configure({
  webClientId: 'your-web-client-id.apps.googleusercontent.com',
  androidClientId: 'your-android-client-id.apps.googleusercontent.com',
  iosClientId: 'your-ios-client-id.apps.googleusercontent.com',
});

// Automatic detection (fallback only)
RnGoogleSignin.configure({});
```

### Basic Usage

```typescript
// Sign in
try {
  const userInfo = await RnGoogleSignin.signIn({});
  console.log('User info:', userInfo);
} catch (error) {
  console.error('Sign in error:', error);
}

// Sign out
await RnGoogleSignin.signOut();
```

**⚠️ Important**: Due to React Native TurboModule requirements, all method parameters must be explicitly passed, even when using default values.

```typescript
// ✅ Correct - always pass the parameter
await RnGoogleSignin.signIn({});
await RnGoogleSignin.hasPlayServices({});

// ❌ Incorrect - will cause runtime errors
await RnGoogleSignin.signIn();
await RnGoogleSignin.hasPlayServices();
```

**Note**: When using default configuration, pass `{}` as the parameter. When using custom options, pass an object with the desired configuration.

## Configuration

### Manual Configuration (Recommended)

Provide your Google client IDs explicitly:

```typescript
RnGoogleSignin.configure({
  // Android (Credential Manager uses Web client ID)
  webClientId: 'your-web-client-id.apps.googleusercontent.com',
  androidClientId: 'your-android-client-id.apps.googleusercontent.com', // optional alias

  // iOS
  iosClientId: 'your-ios-client-id.apps.googleusercontent.com',
});
```

### Automatic Detection (Fallback)

The library can automatically detect client IDs from configuration files as a fallback:

**Android:**
- `google-services.json` in `android/app/` directory
- Uses Google Services plugin to generate `R.string.default_web_client_id`

**iOS:**
- `GoogleService-Info.plist` in your iOS project
- Extracts `CLIENT_ID` from the plist file

**Note**: Automatic detection is provided as a convenience fallback. For production apps, explicit configuration is recommended for better control and reliability.

## Expo Configuration

### Automatic Mode (Default)

Add the plugin without options to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": [
      "@novastera-oss/rn-google-signin"
    ]
  }
}
```

**Requirements for Automatic Mode:**
- Place `google-services.json` in your **project root** (same level as `app.json`)
- Place `GoogleService-Info.plist` in your **project root** (same level as `app.json`)
- Reference them in your `app.json` configuration:

```json
{
  "expo": {
    "ios": {
      "googleServicesFile": "./GoogleService-Info.plist"
    },
    "android": {
      "googleServicesFile": "./google-services.json"
    },
    "plugins": [
      [
        "@novastera-oss/rn-google-signin",
      ]
    ]
  }
}
```

### Manual Mode

Add the plugin with options to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": [
      [
        "@novastera-oss/rn-google-signin",
        {
          "iosUrlScheme": "com.googleusercontent.apps.your-ios-client-id"
        }
      ]
    ]
  }
}
```

**Requirements for Manual Mode:**
- The plugin only adds URL schemes, no file handling
- If you don't provide client IDs in your configuration, you must manually add `GoogleService-Info.plist` to your iOS project

**Complete Manual Configuration Example:**

```json
{
  "expo": {
    "ios": {
      "bundleIdentifier": "com.yourcompany.yourapp",
      "supportsTablet": true,
      "infoPlist": {
        "CFBundleURLTypes": [
          {
            "CFBundleURLSchemes": [
              "com.googleusercontent.apps.your-ios-client-id"
            ]
          },
          {
            "CFBundleURLSchemes": [
              "com.yourcompany.yourapp"
            ]
          }
        ],
        "GIDClientID": "your-ios-client-id.apps.googleusercontent.com",
        "UIBackgroundModes": ["fetch", "remote-notification"]
      },
      "entitlements": {
        "aps-environment": "development"
      }
    }
  }
}
```

**Note**: Replace `your-ios-client-id` with your actual iOS client ID from Google Cloud Console.

## Platform-Specific Features

### iOS
- **Full Scope Support**: Add additional scopes after sign-in
- **Complete Token Management**: Access to both access and ID tokens
- **Rich User Profile**: Complete user information including photos

### Android
- **Modern Credential Manager**: Uses Google's latest Credential Manager API
- **Optimized Performance**: Thread-safe operations with intelligent caching
- **Basic Authentication**: Focused on core sign-in functionality
- **⚠️ Important**: `isSignedIn()` may trigger the credential picker UI even when the user is already signed in. This is a limitation of Google's Credential Manager API. Consider implementing your own sign-in state management for better user experience.

### Platform Differences

| Feature | iOS | Android | Notes |
|---------|-----|---------|-------|
| Basic Sign-In | ✅ | ✅ | |
| Sign Out | ✅ | ✅ | |
| Get Current User | ✅ | ✅ | |
| isSignedIn | ✅ | ⚠️ | Android: may trigger credential picker UI |
| Add Scopes | ✅ | ❌ | Android: Credential Manager limitation |
| Access Token | ✅ | ⚠️ | Android: same as ID token |
| Custom Scopes | ✅ | ❌ | Android: not supported |

## Performance Optimizations

This library includes several performance optimizations:

### Thread Safety
- **Atomic Operations**: Uses `AtomicReference` for thread-safe promise management
- **Single Executor**: Consistent threading model with dedicated executor
- **Weak References**: Prevents memory leaks with proper lifecycle management

### Resource Management
- **Credential Manager Caching**: Avoids recreation on every operation by caching with activity matching
- **Activity Validation**: Smart activity state checking with early returns
- **Promise Deduplication**: Prevents concurrent operations by cancelling previous promises

### Configuration Management
- **Manual Configuration Priority**: Explicit client IDs take precedence over automatic detection
- **Automatic Detection Fallback**: Graceful fallback to config files when manual config not provided
- **Multiple Resource Names**: Tries various resource names for compatibility
- **Graceful Fallback**: Never crashes, always falls back gracefully

## API Reference

### Configuration

```typescript
interface ConfigureParams {
  webClientId?: string;        // Web client ID
  androidClientId?: string;    // Android client ID
  iosClientId?: string;        // iOS client ID
  scopes?: string[];          // Initial scopes (iOS only)
}
```

### User Object

```typescript
interface User {
  id: string;
  name: string;
  email: string;
  photo?: string;
  familyName?: string;
  givenName?: string;
}
```

### Sign In Response

```typescript
interface SignInResponse {
  user: User;
  scopes: string[];
  serverAuthCode?: string;
  idToken?: string;
}
```

## Error Handling

```typescript
try {
  await RnGoogleSignin.signIn({});
} catch (error: any) {
  switch (error.code) {
    case 'sign_in_cancelled':
      // User cancelled
      break;
    case 'sign_in_required':
      // Sign in required (e.g. silent sign-in had no session)
      break;
    case 'no_credential':
      // Android: no Google account on device, or app not registered in Google Cloud (missing Android OAuth client with package name + SHA-1)
      break;
    case 'not_configured':
      // Not configured
      break;
    case 'network_error':
      // Network error
      break;
    default:
      console.error('Sign in error:', error);
  }
}
```

## Troubleshooting

### Common Issues

1. **"No client ID found"**: Ensure configuration files are in the correct location or pass `webClientId` in `configure()`.
2. **"No activity available"**: Make sure the app is in the foreground.
3. **"TurboModule not found"**: Ensure New Architecture is enabled.
4. **Android: "No Google account found" / `NoCredentialException`** (even with an account on the device): Your app must have an **Android OAuth client** in Google Cloud with this app’s **package name** and **SHA-1**. See [Android Setup](#android-setup) above.

### Enable New Architecture

```bash
# iOS
cd ios && RCT_NEW_ARCH_ENABLED=1 bundle exec pod install

# Android - Add to android/gradle.properties
newArchEnabled=true
```

### Android Setup

1. Place `google-services.json` in `android/app/` (required for automatic config; optional if you pass `webClientId` in `configure()`).
2. Ensure Google Play Services is installed on the device or emulator.
3. Add a Google account in device settings (Settings → Accounts → Add account → Google) so the user can sign in.
4. **Create an Android OAuth client** in [Google Cloud Console](https://console.cloud.google.com/apis/credentials) (same project as your Web client ID):
   - **Create credentials** → **OAuth client ID** → Application type **Android**
   - **Package name:** your app’s `applicationId` from `android/app/build.gradle`
   - **SHA-1:** run `cd android && ./gradlew signingReport` (use the fingerprint for the build type you run, e.g. debug)
   - Without this, you can get **"No Google account found"** / `NoCredentialException` even when the device has a Google account, because the app is not authorized for sign-in.

### iOS Setup

1. Add `GoogleService-Info.plist` to your iOS project
2. Ensure bundle identifier matches Google Cloud Console
3. Enable Google Sign-In capability

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## FAQ

### Q: Google Sign-In popup doesn't show in APK builds but works in development

**A:** This is a common issue with production APK builds. Add these permissions to your app's `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application>
        <!-- Your app configuration -->
    </application>
</manifest>
```

Also ensure your package name in `android/app/build.gradle` matches your Google Cloud Console configuration and add your production SHA-1 fingerprint:

```bash
cd android && ./gradlew signingReport
```

**Note:** For apps already published on Google Play Store, you can also find the SHA-1 fingerprint in the Play Console under **Release > Setup > App integrity**.

### Q: Why does `isSignedIn()` trigger the credential picker on Android?

**A:** This is a limitation of Google's Credential Manager API. Consider implementing your own sign-in state management for better user experience.

### Q: Why doesn't the library include a UI button, secure storage, or token parsing?

**A:** The library focuses on providing the core Google Sign-In functionality while keeping things simple. Here's the reasoning behind these design decisions:

**No UI Button:**
- UI components would need customization to match different app designs
- Most apps already have their own design systems and UI components
- Keeps the package focused on authentication logic

**Built-in Secure Storage:**
- **iOS**: Google Sign-In SDK uses Keychain Services for secure token storage
- **Android**: Credential Manager uses Encrypted SharedPreferences with AES-256 encryption
- Both platforms have built-in token management with secure storage
- No need for additional storage libraries for basic token management
- You can still use additional storage libraries if you need custom features

**No Token Parsing:**
- JavaScript has built-in functions to parse JSON efficiently
- Different applications handle tokens differently based on their needs
- Would add another dependency and create parsing you might re-parse anyway
- Keeps the package lightweight

This approach aims to keep dependencies minimal while providing the raw data you need to implement your app's specific requirements.

### Q: Why is the iOS implementation in Objective-C instead of Swift?

**A:** TurboModules don't support Swift natively yet. Using Swift would require the code to be bridged to Objective-C, which adds unnecessary overhead and complexity. Objective-C provides direct compatibility with TurboModules without any bridging layer, resulting in better performance and simpler integration.

## About Novastera

**React Native Google Sign-In** is part of the **Novastera** open-source ecosystem, a modern CRM/ERP platform designed for the next generation of business applications. Novastera combines cutting-edge authentication solutions with comprehensive business management tools, enabling organizations to leverage modern social login for seamless user experiences and enhanced security.

### Key Features of Novastera Platform

- **Modern CRM/ERP System**: Comprehensive business management with AI-powered insights and real-time collaboration
- **Secure Authentication**: Enterprise-grade authentication with Google Sign-In, OAuth 2.0, and social login support
- **Mobile-First**: Native iOS and Android applications built with React Native and Turbo Modules
- **Open Source**: Part of Novastera's commitment to open-source innovation and developer-friendly solutions
- **Privacy-Focused**: On-device AI capabilities with no data leaving user devices

This library is currently being used in [Novastera's](https://novastera.com) mobile application, demonstrating its capabilities in production environments. We're committed to providing modern, secure authentication solutions that stay up-to-date with the latest Google Sign-In SDK features and React Native best practices, helping developers build applications with seamless social login experiences.

Learn more about Novastera: [https://novastera.com/resources](https://novastera.com/resources)

## License

Apache License - see [LICENSE](LICENSE) file for details.