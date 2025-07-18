# React Native Google Sign-In

A modern React Native Google Sign-In library with **Turbo Modules support**

> **⚠️ Important**: This package requires React Native **New Architecture (TurboModules)** and does **NOT** support the legacy bridge architecture.

## Features

- **Modern Google Identity Library**: Uses the latest Google Identity library with Credential Manager and AuthorizationClient
- **New Architecture Only**: Built exclusively for React Native TurboModules (New Architecture)
- **Turbo Modules**: Built with React Native's new Turbo Modules architecture for better performance
- **TypeScript Support**: Full TypeScript support with comprehensive type definitions
- **Expo Plugin**: Includes an Expo config plugin for easy integration
- **Cross-Platform**: Supports both iOS and Android
- **Security**: Built-in nonce support for enhanced security

## Requirements

- **React Native 0.79+** with New Architecture enabled
- **iOS 15.1+**
- **Android API 24+**
- **TurboModules enabled** (`RCT_NEW_ARCH_ENABLED=1`)

### Architecture Support

| Architecture | Support | Notes |
|-------------|---------|-------|
| **New Architecture (TurboModules)** | ✅ **Fully Supported** | Primary target |
| **Old Bridge Architecture** | ❌ **Not Supported** | Not in library scope |

## Installation

### React Native CLI

```bash
npm install @novastera-oss/rn-google-signin
# or
yarn add @novastera-oss/rn-google-signin
```

### Expo

```bash
npx expo install @novastera-oss/rn-google-signin
```

Add the plugin to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": [
      [
        "@novastera-oss/rn-google-signin",
        {
          "iosUrlScheme": "your-ios-bundle-id"
        }
      ]
    ]
  }
}
```

### Complete Expo Configuration

Here's a complete example of the iOS configuration in your `app.json` or `app.config.js`:

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

**Note**: Replace `your-ios-bundle-id` with your actual iOS bundle identifier (e.g., `com.yourcompany.yourapp`). This should be a short, specific identifier for iOS sign-in.

## Setup

### 1. Enable New Architecture

Ensure your React Native project has the New Architecture enabled:

```bash
# For iOS
cd ios && RCT_NEW_ARCH_ENABLED=1 bundle exec pod install

# For Android
# Add to android/gradle.properties
newArchEnabled=true
```

### 2. Google Cloud Console Setup

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google Sign-In API
4. Create OAuth 2.0 credentials:
   - **Web client ID**: For server-side authentication
   - **Android client ID**: For Android apps
   - **iOS client ID**: For iOS apps

### 3. Android Setup

Add your `google-services.json` file to `android/app/`.

### 4. iOS Setup

Add your `GoogleService-Info.plist` file to your iOS project.

## Usage

### Basic Configuration

```typescript
import { GoogleSignin } from '@novastera-oss/rn-google-signin';

// Configure the library
await GoogleSignin.configure({
  webClientId: 'your-web-client-id.apps.googleusercontent.com',
  androidClientId: 'your-android-client-id.apps.googleusercontent.com',
  iosClientId: 'your-ios-client-id.apps.googleusercontent.com',
  scopes: ['email', 'profile'],
  offlineAccess: true,
});
```

### Sign In

```typescript
import { GoogleSignin } from '@novastera-oss/rn-google-signin';

try {
  // Check if user is signed in
  const isSignedIn = await GoogleSignin.isSignedIn();
  
  if (!isSignedIn) {
    // Sign in with default configuration
    const userInfo = await GoogleSignin.signIn(null);
    console.log('User info:', userInfo);
    
    // Or sign in with custom options
    const userInfoWithOptions = await GoogleSignin.signIn({
      scopes: ['email', 'profile'],
      nonce: 'your-custom-nonce-for-security-validation'
    });
    console.log('User info with options:', userInfoWithOptions);
  } else {
    // Get current user
    const currentUser = await GoogleSignin.getCurrentUser();
    console.log('Current user:', currentUser);
  }
} catch (error) {
  console.error('Sign in error:', error);
}
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

### Security with Nonce

For enhanced security, you can provide a custom nonce when signing in:

```typescript
// Generate a secure nonce on your server
const serverNonce = generateSecureNonce();

// Pass it to the sign-in method
const userInfo = await GoogleSignin.signIn({
  nonce: serverNonce
});

// Validate the nonce in your server-side token verification
```

If no nonce is provided, the library will generate one automatically.

## Platform-Specific Features

### iOS Features
- **Full Scope Support**: iOS supports adding additional scopes after sign-in using `addScopes()`
- **Token Management**: Full access to access tokens and ID tokens
- **User Profile**: Complete user profile information including photos

### Android Features
- **Modern Credential Manager**: Uses Google's latest Credential Manager API
- **Basic Authentication**: Focused on core sign-in functionality
- **Limited Scope Support**: Android implementation uses Credential Manager which has limited scope support

### Cross-Platform Compatibility

The library provides a unified API across both platforms, but some features have platform-specific limitations:

```typescript
// ✅ Works on both platforms
await GoogleSignin.signIn(null);
await GoogleSignin.isSignedIn();
await GoogleSignin.getCurrentUser();

// ✅ iOS only - adding scopes after sign-in
const result = await GoogleSignin.addScopes([
  'https://www.googleapis.com/auth/drive.readonly'
]);

// ⚠️ Android - addScopes() is not supported
// Will throw "not_supported" error on Android
```

### Silent Sign In

```typescript
try {
  const userInfo = await GoogleSignin.signInSilently();
  console.log('Silent sign in successful:', userInfo);
} catch (error) {
  console.error('Silent sign in failed:', error);
}
```

### Sign Out

```typescript
try {
  await GoogleSignin.signOut();
  console.log('User signed out');
} catch (error) {
  console.error('Sign out error:', error);
}
```

### Get Tokens

```typescript
try {
  const tokens = await GoogleSignin.getTokens();
  console.log('Access token:', tokens.accessToken);
  console.log('ID token:', tokens.idToken);
} catch (error) {
  console.error('Get tokens error:', error);
}
```

## API Reference

### Configuration

```typescript
interface ConfigureParams {
  webClientId?: string;        // Required for Android, optional for iOS
  androidClientId?: string;     // Alternative to webClientId for Android
  iosClientId?: string;         // iOS-specific client ID
  scopes?: string[];           // Initial scopes (iOS only)
  offlineAccess?: boolean;      // Not used in current implementation
  hostedDomain?: string;        // Not used in current implementation
  forceCodeForRefreshToken?: boolean; // Not used in current implementation
  accountName?: string;         // Not used in current implementation
  googleServicePlistPath?: string; // Not used in current implementation
  openIdRealm?: string;        // Not used in current implementation
  profileImageSize?: number;    // Not used in current implementation
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

### Error Handling

The library provides consistent error codes across iOS and Android platforms:

```typescript
import { GoogleSignin, GoogleSignInErrorCode } from '@novastera-oss/rn-google-signin';

try {
  await GoogleSignin.signIn(null);
} catch (error: any) {
  switch (error.code as GoogleSignInErrorCode) {
    case 'sign_in_cancelled':
      // User cancelled the sign in
      break;
    case 'sign_in_required':
      // Sign in required (for silent sign in)
      break;
    case 'not_configured':
      // Google Sign In is not configured
      break;
    case 'no_credential':
      // No credential available
      break;
    case 'network_error':
      // Network error occurred
      break;
    case 'configuration_error':
      // Configuration error (missing client ID, etc.)
      break;
    case 'token_error':
      // Token-related error
      break;
    case 'keychain_error':
      // Keychain error (iOS only)
      break;
    case 'play_services_not_available':
      // Play services not available (Android only)
      break;
    case 'internal_error':
      // Internal error
      break;
    default:
      // Handle other error codes
      console.error('Unknown error:', error.code);
      break;
  }
}
```

#### Error Codes

The following error codes are supported across platforms:

**Common Error Codes:**
- `sign_in_cancelled` - User cancelled the sign in
- `sign_in_required` - Sign in required (for silent sign in)
- `sign_in_error` - Generic sign in error
- `not_configured` - Google Sign In is not configured
- `no_activity` - No current activity available
- `no_credential` - No credential available
- `network_error` - Network error
- `unknown_error` - Unknown error occurred

**Platform-Specific Error Codes:**
- `play_services_not_available` - Play services not available (Android only)
- `parsing_error` - Failed to parse Google ID token (Android only)
- `not_supported` - Feature not supported (Android only, for addScopes)
- `invalid_scopes` - Invalid scopes provided (iOS only)
- `native_crash` - Native code crashed (iOS only)

## Troubleshooting

### Common Issues

1. **"No client ID found"**: Ensure you've provided the correct client IDs in the configuration
2. **"No activity available"**: Make sure the app is in the foreground when calling sign-in methods
3. **"Google Sign In is not configured"**: Call `configure()` before using any sign-in methods
4. **"TurboModule not found"**: Ensure New Architecture is enabled and TurboModules are properly configured
5. **"Feature not supported"**: Some features like `addScopes()` are only available on iOS

### Architecture Issues

If you're getting errors related to TurboModules or the New Architecture:

1. **Ensure New Architecture is enabled**:
   ```bash
   # iOS
   cd ios && RCT_NEW_ARCH_ENABLED=1 bundle exec pod install
   
   # Android
   # Add to android/gradle.properties
   newArchEnabled=true
   ```

2. **Check React Native version**: This package requires React Native 0.79+

### Promise Handling

The library uses unified promise handling to prevent race conditions:

- **Single Promise Management**: Only one authentication operation can be active at a time
- **Automatic Cancellation**: New requests automatically cancel previous pending operations
- **Error Handling**: Comprehensive error handling with platform-specific error codes

### Android Issues

- Ensure `google-services.json` is properly placed in `android/app/`
- Check that your package name matches the one in Google Cloud Console
- Verify that Google Play Services is available on the device
- Note: `addScopes()` is not supported on Android due to Credential Manager limitations

### iOS Issues

- Ensure `GoogleService-Info.plist` is added to your iOS project
- Check that your bundle identifier matches the one in Google Cloud Console
- Verify that the Google Sign-In capability is enabled in your app
- For additional scopes, use `addScopes()` after initial sign-in

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) file for details.
