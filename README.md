# React Native Google Sign-In

A modern React Native Google Sign-In library with Turbo Modules support, built with the latest Google Identity library.

## Features

- **Modern Google Identity Library**: Uses the latest Google Identity library with Credential Manager and AuthorizationClient
- **No Legacy Dependencies**: Completely removes deprecated Google Sign-In SDK (removed from Google Play Services in 2025)
- **Turbo Modules**: Built with React Native's new Turbo Modules architecture for better performance
- **TypeScript Support**: Full TypeScript support with comprehensive type definitions
- **Expo Plugin**: Includes an Expo config plugin for easy integration
- **Cross-Platform**: Supports both iOS and Android
- **Async/Await**: Modern Promise-based API
- **Security**: Built-in nonce support for enhanced security
- **Race Condition Free**: Single promise management prevents conflicts

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
      "@novastera-oss/rn-google-signin"
    ]
  }
}
```

## Setup

### 1. Google Cloud Console Setup

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google Sign-In API
4. Create OAuth 2.0 credentials:
   - **Web client ID**: For server-side authentication
   - **Android client ID**: For Android apps
   - **iOS client ID**: For iOS apps

### 2. Android Setup

Add your `google-services.json` file to `android/app/`.

### 3. iOS Setup

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
    // Sign in with custom nonce for security
    const userInfo = await GoogleSignin.signIn({
      scopes: ['email', 'profile'],
      nonce: 'your-custom-nonce-for-security-validation'
    });
    console.log('User info:', userInfo);
  } else {
    // Get current user
    const currentUser = await GoogleSignin.getCurrentUser();
    console.log('Current user:', currentUser);
  }
} catch (error) {
  console.error('Sign in error:', error);
}
```

**Important**: Due to React Native TurboModule requirements, optional parameters must always be passed as `null` if there is no value, `{}` could work too for objects

```typescript
// ✅ Correct - always pass the parameter
await GoogleSignin.signIn(null);
await GoogleSignin.hasPlayServices(null);

// ❌ Incorrect - will cause runtime errors
await GoogleSignin.signIn();
await GoogleSignin.hasPlayServices();
```

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

## Migration from Legacy Google Sign-In

This library uses the **modern Google Identity APIs** and completely removes the deprecated Google Sign-In SDK. According to [Google's migration documentation](https://developer.android.com/identity/sign-in/legacy-gsi-migration), Google Sign-In for Android is deprecated and will be removed from Google Play Services Auth SDK in 2025.

### Key Changes:

1. **Authentication**: Uses Credential Manager instead of GoogleSignInClient
2. **Authorization**: Uses AuthorizationClient for scope management
3. **No Legacy Dependencies**: Removes all deprecated Google Sign-In SDK code
4. **Single Promise Management**: Prevents race conditions with unified promise handling

### Migration Steps:

1. Replace the import:
   ```typescript
   // Previous (using deprecated modules)
   import { GoogleSignIn } from '@react-native-google-signin/google-signin';
   
   // New (using modern Google Identity APIs)
   import GoogleSignin from '@novastera-oss/rn-google-signin';
   ```

2. Update configuration (simplified):
   ```typescript
   // Previous (legacy)
   await GoogleSignIn.configure({
     webClientId: 'your-client-id',
     offlineAccess: true, // No longer needed
     scopes: ['email', 'profile'] // Use addScopes() instead
   });
   
   // New (modern)
   await GoogleSignin.configure({
     webClientId: 'your-client-id',
   });
   ```

3. Request scopes when needed:
   ```typescript
   // Request additional scopes when user performs an action
   const scopeResult = await GoogleSignin.addScopes([
     'https://www.googleapis.com/auth/drive.readonly'
   ]);
   ```

**Note**: This library provides a clean, modern implementation using Google's current recommended APIs. It's future-proof and follows Google's latest authentication best practices.

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
  webClientId?: string;
  androidClientId?: string;
  iosClientId?: string;
  scopes?: string[];
  offlineAccess?: boolean;
  hostedDomain?: string;
  forceCodeForRefreshToken?: boolean;
  accountName?: string;
  googleServicePlistPath?: string;
  openIdRealm?: string;
  profileImageSize?: number;
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

## Migration from Old Google Sign-In SDK

This library uses the new Google Identity library instead of the deprecated Google Sign-In SDK. Key differences:

### Benefits of New Library

1. **Better Security**: Uses modern authentication standards
2. **Improved Performance**: More efficient token management
3. **Future-Proof**: Actively maintained by Google
4. **Better Error Handling**: More specific error codes and messages

### API Changes

- `addScopes()`: Not supported in new library (scopes are requested during initial sign-in)
- `signOut()`: No direct method (user must sign out from Google account settings)
- `revokeAccess()`: No direct method (user must revoke from Google account settings)
- `serverAuthCode`: Not available in new library

### Error Handling

The library provides consistent error codes across iOS and Android platforms:

```typescript
import { GoogleSignin, GoogleSignInErrorCode } from '@novastera-oss/rn-google-signin';

try {
  await GoogleSignin.signIn();
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

#### Consistent Error Codes

The following error codes are consistent across both platforms:

- `sign_in_cancelled` - User cancelled the sign in
- `sign_in_required` - Sign in required (for silent sign in)
- `sign_in_error` - Generic sign in error
- `not_configured` - Google Sign In is not configured
- `no_activity` - No current activity available
- `client_error` - Google Sign In client not initialized
- `token_error` - Token-related error
- `add_scopes_error` - Failed to add scopes
- `revoke_error` - Failed to revoke access
- `configuration_error` - Configuration error
- `conversion_error` - Failed to convert user data
- `no_credential` - No credential available
- `unknown_error` - Unknown error occurred
- `network_error` - Network error
- `keychain_error` - Keychain error (iOS only)
- `not_in_keychain` - User not found in keychain (iOS only)
- `scopes_error` - Scope error occurred (iOS only)
- `emm_error` - Enterprise Mobility Management error (iOS only)
- `play_services_not_available` - Play services not available (Android only)
- `parsing_error` - Failed to parse Google ID token (Android only)

## Troubleshooting

### Common Issues

1. **"No client ID found"**: Ensure you've provided the correct client IDs in the configuration
2. **"No activity available"**: Make sure the app is in the foreground when calling sign-in methods
3. **"Credential manager not initialized"**: Call `configure()` before using any sign-in methods
4. **Infinite awaits or no response**: This was fixed in v0.1.0 by separating promise handling for different authentication flows

### Promise Handling

The library uses separate promise handling for different authentication flows to prevent race conditions:

- **Credential Manager flow**: Used for simple sign-ins without custom scopes
- **Google Sign-In SDK flow**: Used for sign-ins with custom scopes or offline access

Each flow has its own promise management to prevent conflicts and infinite awaits.

### Android Issues

- Ensure `google-services.json` is properly placed in `android/app/`
- Check that your package name matches the one in Google Cloud Console
- Verify that Google Play Services is available on the device

### iOS Issues

- Ensure `GoogleService-Info.plist` is added to your iOS project
- Check that your bundle identifier matches the one in Google Cloud Console
- Verify that the Google Sign-In capability is enabled in your app

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) file for details.
