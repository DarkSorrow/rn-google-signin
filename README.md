# React Native Google Sign-In

A modern React Native Google Sign-In library with Turbo Modules support, built with the latest Google Identity library.

## Features

- **Modern Google Identity Library**: Uses the latest Google Identity library instead of the deprecated Google Sign-In SDK
- **Turbo Modules**: Built with React Native's new Turbo Modules architecture for better performance
- **TypeScript Support**: Full TypeScript support with comprehensive type definitions
- **Expo Plugin**: Includes an Expo config plugin for easy integration
- **Cross-Platform**: Supports both iOS and Android
- **Async/Await**: Modern Promise-based API
- **Security**: Built-in nonce support for enhanced security

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

## Migration from Libraries Using Deprecated Modules

This library was created to provide a simple, secure Google Sign-In flow using the latest native libraries. If you're migrating from libraries using deprecated modules:

1. Replace the import:
   ```typescript
   // Previous (using deprecated modules)
   import { GoogleSignIn } from '@react-native-google-signin/google-signin';
   
   // New (using latest native libraries)
   import GoogleSignin from '@novastera-oss/rn-google-signin';
   ```

2. Update method calls to use the new API:
   ```typescript
   // Previous
   await GoogleSignIn.configure({
     webClientId: 'your-client-id',
   });
   
   // New (same API, but with latest native libraries)
   await GoogleSignin.configure({
     webClientId: 'your-client-id',
   });
   ```

**Note**: This library focuses on providing a simple, secure Google Sign-In flow with the latest native libraries. It has fewer features than comprehensive solutions but ensures your authentication uses up-to-date, secure APIs. If you need advanced features, consider the premium offerings from @react-native-google-signin/google-signin who did an amazing job.

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

The new library provides more specific error types:

```typescript
try {
  await GoogleSignin.signIn();
} catch (error) {
  if (error.code === 'sign_in_cancelled') {
    // User cancelled the sign in
  } else if (error.code === 'no_credential') {
    // No credential available
  } else if (error.code === 'parsing_error') {
    // Failed to parse Google ID token
  }
}
```

## Troubleshooting

### Common Issues

1. **"No client ID found"**: Ensure you've provided the correct client IDs in the configuration
2. **"No activity available"**: Make sure the app is in the foreground when calling sign-in methods
3. **"Credential manager not initialized"**: Call `configure()` before using any sign-in methods

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
