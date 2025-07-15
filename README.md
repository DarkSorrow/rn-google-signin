# React Native Google Sign-In with Turbo Modules

A modern React Native Google Sign-In library built with Turbo Native Modules for optimal performance and direct native access.

## Features

- ⚡ **Turbo Native Modules** - Direct native access without JavaScript bridge overhead
- 🔧 **TypeScript Support** - Full type safety and IntelliSense
- 📱 **Cross-Platform** - iOS and Android support
- 🚀 **Modern Architecture** - Built for React Native 0.79+ with New Architecture
- 🔐 **Secure Authentication** - Uses official Google Sign-In SDKs
- 📦 **Expo Compatible** - Works with Expo managed and bare workflows

## Installation

### 1. Install the package

```bash
npm install @novastera-oss/rn-google-signin
# or
yarn add @novastera-oss/rn-google-signin
```

### 2. iOS Setup

#### Using Expo

If you're using Expo, the iOS dependencies will be automatically configured.

#### Manual Setup

1. Install the iOS dependencies:

```bash
cd ios && pod install
```

2. Add your `GoogleService-Info.plist` to the iOS project.

### 3. Android Setup

#### Using Expo

If you're using Expo, the Android dependencies will be automatically configured.

#### Manual Setup

1. Add your `google-services.json` to the `android/app/` directory.

2. Update your `android/build.gradle`:

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}
```

3. Update your `android/app/build.gradle`:

```gradle
apply plugin: 'com.google.gms.google-services'
```

## Usage

### Basic Setup

```typescript
import GoogleSignIn from '@novastera-oss/rn-google-signin';

// Configure the module
await GoogleSignIn.configure({
  webClientId: 'your-web-client-id.apps.googleusercontent.com',
  iosClientId: 'your-ios-client-id.apps.googleusercontent.com', // Optional
  scopes: ['email', 'profile'],
  offlineAccess: true,
});
```

### Sign In

```typescript
import GoogleSignIn from '@novastera-oss/rn-google-signin';

const signIn = async () => {
  try {
    // Check if Google Play Services are available (Android only)
    const hasPlayServices = await GoogleSignIn.hasPlayServices();
    
    if (hasPlayServices) {
      // Sign in the user
      const userInfo = await GoogleSignIn.signIn();
      console.log('User signed in:', userInfo);
    }
  } catch (error) {
    console.error('Sign in error:', error);
  }
};
```

### Silent Sign In

```typescript
const signInSilently = async () => {
  try {
    const userInfo = await GoogleSignIn.signInSilently();
    console.log('Silent sign in successful:', userInfo);
  } catch (error) {
    console.error('Silent sign in error:', error);
  }
};
```

### Get Current User

```typescript
const getCurrentUser = async () => {
  try {
    const user = await GoogleSignIn.getCurrentUser();
    if (user) {
      console.log('Current user:', user);
    } else {
      console.log('No user signed in');
    }
  } catch (error) {
    console.error('Get current user error:', error);
  }
};
```

### Sign Out

```typescript
const signOut = async () => {
  try {
    await GoogleSignIn.signOut();
    console.log('User signed out');
  } catch (error) {
    console.error('Sign out error:', error);
  }
};
```

### Get Tokens

```typescript
const getTokens = async () => {
  try {
    const tokens = await GoogleSignIn.getTokens();
    console.log('Access token:', tokens.accessToken);
    console.log('ID token:', tokens.idToken);
  } catch (error) {
    console.error('Get tokens error:', error);
  }
};
```

## API Reference

### Configuration

```typescript
interface GoogleSignInConfig {
  webClientId?: string;           // Required: Your web client ID
  iosClientId?: string;           // Optional: iOS client ID
  androidClientId?: string;       // Optional: Android client ID
  scopes?: string[];              // Optional: Additional scopes
  offlineAccess?: boolean;        // Optional: Request offline access
  hostedDomain?: string;          // Optional: Restrict to specific domain
  forceCodeForRefreshToken?: boolean; // Optional: Force refresh token
  accountName?: string;           // Optional: Pre-select account
  googleServicePlistPath?: string; // Optional: Path to GoogleService-Info.plist
}
```

### User Object

```typescript
interface GoogleSignInUser {
  user: {
    id: string;
    name: string | null;
    email: string;
    photo: string | null;
    familyName: string | null;
    givenName: string | null;
  };
  scopes: string[];
  serverAuthCode: string | null;
  idToken: string | null;
}
```

### Methods

| Method | Description |
|--------|-------------|
| `configure(config)` | Configure the Google Sign-In module |
| `hasPlayServices()` | Check if Google Play Services are available (Android only) |
| `signIn()` | Sign in the user |
| `signInSilently()` | Sign in silently if user was previously signed in |
| `addScopes(scopes)` | Add additional scopes to the current user |
| `signOut()` | Sign out the current user |
| `revokeAccess()` | Revoke access and sign out the user |
| `isSignedIn()` | Check if a user is currently signed in |
| `getCurrentUser()` | Get the current signed-in user |
| `clearCachedAccessToken(token)` | Clear the cached access token |
| `getTokens()` | Get the current access token and ID token |

### Error Handling

```typescript
import { GoogleSignInError, statusCodes } from '@novastera-oss/rn-google-signin';

try {
  await GoogleSignIn.signIn();
} catch (error) {
  if (error instanceof GoogleSignInError) {
    switch (error.code) {
      case statusCodes.SIGN_IN_CANCELLED:
        console.log('User cancelled sign in');
        break;
      case statusCodes.IN_PROGRESS:
        console.log('Sign in already in progress');
        break;
      case statusCodes.PLAY_SERVICES_NOT_AVAILABLE:
        console.log('Google Play Services not available');
        break;
      case statusCodes.SIGN_IN_REQUIRED:
        console.log('User needs to sign in');
        break;
    }
  }
}
```

## Turbo Modules Benefits

This library uses Turbo Native Modules, which provides several advantages:

1. **Direct Native Access** - No JavaScript bridge overhead
2. **Better Performance** - Faster method calls and data transfer
3. **Type Safety** - Full TypeScript support with proper types
4. **Modern Architecture** - Built for React Native's New Architecture
5. **Future-Proof** - Compatible with upcoming React Native versions

## How Turbo Modules Work

This library follows the React Native Turbo Modules pattern:

1. **TypeScript Spec**: `src/spec/NativeGoogleSignin.ts` defines the interface
2. **Codegen**: React Native automatically generates native interfaces
3. **Native Implementation**: Android (Kotlin) and iOS (Swift) implement the spec
4. **Direct Access**: No JavaScript wrapper - direct native module access

### Codegen Configuration

The `package.json` includes the proper Codegen configuration:

```json
{
  "codegenConfig": {
    "name": "AppSpecs",
    "type": "modules",
    "jsSrcsDir": "src/spec",
    "android": {
      "javaPackageName": "com.novastera.rngooglesignin"
    },
    "ios": {
      "modulesProvider": {
        "NativeGoogleSignin": "RCTNativeGoogleSignin"
      }
    }
  }
}
```

### iOS Module Registration

The iOS implementation includes a module provider (`RNGoogleSigninModuleProvider.swift`) that registers the Turbo Module with React Native's codegen system.

## Migration from Legacy Modules

If you're migrating from a legacy Google Sign-In library:

1. Replace the import:
   ```typescript
   // Old
   import { GoogleSignIn } from 'react-native-google-signin';
   
   // New
   import GoogleSignIn from '@novastera-oss/rn-google-signin';
   ```

2. Update method calls to use the new API:
   ```typescript
   // Old
   await GoogleSignIn.configure({
     webClientId: 'your-client-id',
   });
   
   // New (same API, but with Turbo Module benefits)
   await GoogleSignIn.configure({
     webClientId: 'your-client-id',
   });
   ```

## Troubleshooting

### Common Issues

1. **"Google Sign In is not configured"**
   - Make sure to call `GoogleSignIn.configure()` before any other methods

2. **"No client ID found"**
   - Ensure you have provided a valid `webClientId` or `iosClientId`
   - Check that your `GoogleService-Info.plist` is properly added to the iOS project

3. **"Google Play Services not available"** (Android)
   - This is expected on emulators or devices without Google Play Services
   - The method will return `false` in these cases

4. **Build errors**
   - Make sure you have the latest version of React Native
   - Clean and rebuild your project
   - For iOS: `cd ios && pod install && cd ..`
   - For Android: `cd android && ./gradlew clean && cd ..`

### Debug Mode

Enable debug logging by setting the environment variable:

```bash
export GOOGLE_SIGNIN_DEBUG=true
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

- 📖 [Documentation](https://github.com/novastera/rn-google-signin#readme)
- 🐛 [Issues](https://github.com/novastera/rn-google-signin/issues)
- 💬 [Discussions](https://github.com/novastera/rn-google-signin/discussions)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes and version history.
