# React Native Google Sign-In

A modern, high-performance React Native Google Sign-In library built exclusively for **Turbo Modules** with automatic configuration detection.

> **⚠️ Important**: This package requires React Native **New Architecture (TurboModules)** and does **NOT** support the legacy bridge architecture.

## Features

- **🚀 Turbo Modules**: Built with React Native's new architecture for maximum performance
- **🔧 Automatic Configuration**: Auto-detects client IDs from `google-services.json` and `GoogleService-Info.plist`
- **📱 Cross-Platform**: Full support for iOS and Android with platform-optimized implementations
- **🔒 Security First**: Built-in nonce support and secure credential management
- **⚡ Optimized Performance**: Thread-safe operations with intelligent caching and resource management
- **📦 TypeScript**: Complete type definitions and excellent developer experience
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
import { GoogleSignin } from '@novastera-oss/rn-google-signin';

// Manual configuration (recommended)
await GoogleSignin.configure({
  webClientId: 'your-web-client-id.apps.googleusercontent.com',
  androidClientId: 'your-android-client-id.apps.googleusercontent.com',
  iosClientId: 'your-ios-client-id.apps.googleusercontent.com',
});

// Automatic detection (fallback only)
await GoogleSignin.configure({});
```

### Basic Usage

```typescript
// Sign in
try {
  const userInfo = await GoogleSignin.signIn(null);
  console.log('User info:', userInfo);
} catch (error) {
  console.error('Sign in error:', error);
}

// Sign out
await GoogleSignin.signOut();
```

**⚠️ Important**: Due to React Native TurboModule requirements, all method parameters must be explicitly passed, even when using default values.

```typescript
// ✅ Correct - always pass the parameter
await GoogleSignin.signIn(null);
await GoogleSignin.hasPlayServices(null);

// ❌ Incorrect - will cause runtime errors
await GoogleSignin.signIn();
await GoogleSignin.hasPlayServices();
```

**Note**: When using default configuration, pass `null` as the parameter. When using custom options, pass an object with the desired configuration.

## Configuration

### Manual Configuration (Recommended)

Provide your Google client IDs explicitly:

```typescript
await GoogleSignin.configure({
  // Android (choose one)
  androidClientId: 'your-android-client-id.apps.googleusercontent.com',
  webClientId: 'your-web-client-id.apps.googleusercontent.com',
  
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
    }
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
  await GoogleSignin.signIn(null);
} catch (error: any) {
  switch (error.code) {
    case 'sign_in_cancelled':
      // User cancelled
      break;
    case 'sign_in_required':
      // Sign in required
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

1. **"No client ID found"**: Ensure configuration files are in the correct location
2. **"No activity available"**: Make sure the app is in the foreground
3. **"TurboModule not found"**: Ensure New Architecture is enabled

### Enable New Architecture

```bash
# iOS
cd ios && RCT_NEW_ARCH_ENABLED=1 bundle exec pod install

# Android - Add to android/gradle.properties
newArchEnabled=true
```

### Android Setup

1. Place `google-services.json` in `android/app/`
2. Ensure Google Play Services is installed
3. Add a Google account in device settings

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

## License

Apache License - see [LICENSE](LICENSE) file for details.