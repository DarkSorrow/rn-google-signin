# @novastera-oss/rn-google-signin

Modern React Native Google Sign-In with Turbo Modules support for the new architecture.

> **Note**: This package was created to address specific security and architectural requirements. The excellent `@react-native-google-signin/google-signin` library offers many more features and premium functionality. If you're using their premium features, please continue supporting their work.

[![npm version](https://badge.fury.io/js/%40novastera%2Frn-google-signin.svg)](https://badge.fury.io/js/%40novastera%2Frn-google-signin)
[![React Native](https://img.shields.io/badge/React%20Native-0.79%2B-blue)](https://reactnative.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-Ready-blue)](https://www.typescriptlang.org/)
[![Expo](https://img.shields.io/badge/Expo-Compatible-black)](https://expo.dev/)

## Features

- ✅ **Turbo Modules** - Built for React Native's new architecture
- ✅ **TypeScript** - Full TypeScript support with strict typing
- ✅ **Modern APIs** - Uses latest Google Identity Services
- ✅ **Expo Compatible** - Works with Expo managed workflow via config plugin
- ✅ **iOS & Android** - Full platform support
- ✅ **Secure** - Follows Google's latest security best practices
- ✅ **Well Documented** - Comprehensive guides and API reference (WIP)
- ✅ **Centralized Dependencies** - Single source of truth for all versions
- ✅ **Git Submodules** - Efficient iOS SDK management

## Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
  - [React Native CLI](#react-native-cli)
  - [Expo](#expo)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Migration from react-native-google-signin](#migration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Installation

```bash
npm install @novastera-oss/rn-google-signin
```

or

```bash
yarn add @novastera-oss/rn-google-signin
```

## Configuration

### React Native CLI

#### iOS Setup

1. **Set up iOS dependencies** (using Git submodules):
   
   **macOS/Linux:**
   ```bash
   npm run setup-ios-deps
   ```
   
   **Windows:**
   ```bash
   npm run setup-ios-deps:windows
   ```
   
   This command will:
   - Add the GoogleSignIn iOS SDK as a Git submodule
   - Pin it to a specific commit for stability
   - Set up the proper directory structure

   > **Note**: This package uses Git submodules to manage iOS dependencies efficiently. If you clone this repository, make sure to initialize submodules:
   > ```bash
   > git submodule update --init --recursive
   > ```

2. **Add your GoogleService-Info.plist**:
   - Download from [Firebase Console](https://console.firebase.google.com/)
   - Add to your iOS project in Xcode

3. **Configure URL Scheme**:
   Add this to your `ios/YourApp/Info.plist`:
   ```xml
   <key>CFBundleURLTypes</key>
   <array>
     <dict>
       <key>CFBundleURLName</key>
       <string>GoogleSignIn</string>
       <key>CFBundleURLSchemes</key>
       <array>
         <!-- Replace with your REVERSED_CLIENT_ID from GoogleService-Info.plist -->
         <string>com.googleusercontent.apps.YOUR_CLIENT_ID</string>
       </array>
     </dict>
   </array>
   ```

#### Android Setup

1. **Add your google-services.json**:
   - Download from [Firebase Console](https://console.firebase.google.com/)
   - Place in `android/app/google-services.json`

2. **Configure Gradle**:
   Add to `android/app/build.gradle`:
   ```gradle
   apply plugin: 'com.google.gms.google-services'
   ```

   Add to `android/build.gradle`:
   ```gradle
   dependencies {
     classpath 'com.google.gms:google-services:4.3.15'
   }
   ```

### Expo

Install the package and configure the plugin:

```bash
npx expo install @novastera-oss/rn-google-signin
```

Add the plugin to your `app.config.js`:

```javascript
export default {
  expo: {
    // ... your existing config
    plugins: [
      [
        "@novastera-oss/rn-google-signin",
        {
          iosClientId: "YOUR_IOS_CLIENT_ID.apps.googleusercontent.com",
          androidClientId: "YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com",
          webClientId: "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
        }
      ]
    ]
  }
};
```

Then rebuild your development build:

```bash
npx expo run:ios
npx expo run:android
```

## Usage

### Basic Setup

```typescript
import GoogleSignIn, { GoogleSignInConfig } from '@novastera-oss/rn-google-signin';

const config: GoogleSignInConfig = {
  webClientId: 'YOUR_WEB_CLIENT_ID.apps.googleusercontent.com',
  offlineAccess: true,
  scopes: ['profile', 'email'],
};

// Configure the library
await GoogleSignIn.configure(config);
```

### Sign In

```typescript
import GoogleSignIn, { GoogleSignInUser } from '@novastera-oss/rn-google-signin';

const handleSignIn = async () => {
  try {
    // Check if device has Google Play Services (Android)
    await GoogleSignIn.hasPlayServices();
    
    // Trigger the sign-in flow
    const user: GoogleSignInUser = await GoogleSignIn.signIn();
    
    console.log('User signed in:', user);
    // Use user.user.email, user.user.name, etc.
  } catch (error) {
    console.error('Sign-in error:', error);
  }
};
```

### Silent Sign In

```typescript
const handleSilentSignIn = async () => {
  try {
    const user = await GoogleSignIn.signInSilently();
    console.log('Silent sign-in successful:', user);
  } catch (error) {
    console.log('No existing sign-in found');
  }
};
```

### Sign Out

```typescript
const handleSignOut = async () => {
  try {
    await GoogleSignIn.signOut();
    console.log('User signed out');
  } catch (error) {
    console.error('Sign-out error:', error);
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
      console.log('No user is signed in');
    }
  } catch (error) {
    console.error('Error getting current user:', error);
  }
};
```

### Complete Example

```typescript
import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import GoogleSignIn, { 
  GoogleSignInUser, 
  GoogleSignInConfig,
  statusCodes 
} from '@novastera-oss/rn-google-signin';

const App = () => {
  const [user, setUser] = useState<GoogleSignInUser | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);

  useEffect(() => {
    // Configure Google Sign-In
    const configure = async () => {
      try {
        const config: GoogleSignInConfig = {
          webClientId: 'YOUR_WEB_CLIENT_ID.apps.googleusercontent.com',
          offlineAccess: true,
          scopes: ['profile', 'email'],
        };
        
        await GoogleSignIn.configure(config);
        
        // Try silent sign-in
        const currentUser = await GoogleSignIn.getCurrentUser();
        setUser(currentUser);
      } catch (error) {
        console.error('Configuration error:', error);
      }
    };

    configure();
  }, []);

  const handleSignIn = async () => {
    setIsSigningIn(true);
    try {
      await GoogleSignIn.hasPlayServices();
      const userInfo = await GoogleSignIn.signIn();
      setUser(userInfo);
    } catch (error: any) {
      if (error.code === statusCodes.SIGN_IN_CANCELLED) {
        Alert.alert('Sign-in cancelled');
      } else if (error.code === statusCodes.IN_PROGRESS) {
        Alert.alert('Sign-in in progress');
      } else if (error.code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
        Alert.alert('Play Services not available');
      } else {
        Alert.alert('Error', error.message);
      }
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleSignOut = async () => {
    try {
      await GoogleSignIn.signOut();
      setUser(null);
    } catch (error) {
      console.error('Sign-out error:', error);
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      {user ? (
        <View style={{ alignItems: 'center' }}>
          <Text>Welcome, {user.user.name}!</Text>
          <Text>{user.user.email}</Text>
          <TouchableOpacity
            onPress={handleSignOut}
            style={{ marginTop: 20, padding: 10, backgroundColor: '#db4437' }}
          >
            <Text style={{ color: 'white' }}>Sign Out</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <TouchableOpacity
          onPress={handleSignIn}
          disabled={isSigningIn}
          style={{ padding: 10, backgroundColor: '#4285f4' }}
        >
          <Text style={{ color: 'white' }}>
            {isSigningIn ? 'Signing In...' : 'Sign In with Google'}
          </Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

export default App;
```

## API Reference

### Configuration

#### `GoogleSignIn.configure(config: GoogleSignInConfig): Promise<void>`

Configure the Google Sign-In instance.

**Parameters:**
- `config`: Configuration object

**GoogleSignInConfig:**
```typescript
interface GoogleSignInConfig {
  webClientId?: string;          // Web client ID from Google Console
  iosClientId?: string;          // iOS client ID (optional)
  androidClientId?: string;      // Android client ID (optional)
  scopes?: string[];             // Additional scopes to request
  offlineAccess?: boolean;       // Request offline access
  hostedDomain?: string;         // Restrict to specific domain
  forceCodeForRefreshToken?: boolean;  // Force server auth code
  accountName?: string;          // Pre-select account (Android)
  googleServicePlistPath?: string;     // Custom plist path (iOS)
}
```

### Authentication Methods

#### `GoogleSignIn.signIn(): Promise<GoogleSignInUser>`

Initiate the sign-in flow.

#### `GoogleSignIn.signInSilently(): Promise<GoogleSignInUser>`

Sign in silently if user is already authenticated.

#### `GoogleSignIn.signOut(): Promise<void>`

Sign out the current user.

#### `GoogleSignIn.revokeAccess(): Promise<void>`

Revoke access and sign out.

### User Information

#### `GoogleSignIn.isSignedIn(): Promise<boolean>`

Check if a user is currently signed in.

#### `GoogleSignIn.getCurrentUser(): Promise<GoogleSignInUser | null>`

Get current user information.

**GoogleSignInUser:**
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

### Utility Methods

#### `GoogleSignIn.hasPlayServices(): Promise<boolean>`

Check if Google Play Services are available (Android only, returns `true` on iOS).

#### `GoogleSignIn.addScopes(scopes: string[]): Promise<GoogleSignInUser>`

Request additional scopes from the current user.

#### `GoogleSignIn.getTokens(): Promise<{ accessToken: string; idToken: string | null }>`

Get current access token and ID token.

#### `GoogleSignIn.clearCachedAccessToken(accessToken: string): Promise<void>`

Clear cached access token (Android only).

### Error Handling

```typescript
import { statusCodes, GoogleSignInError } from '@novastera-oss/rn-google-signin';

// Status codes
statusCodes.SIGN_IN_CANCELLED      // User cancelled
statusCodes.IN_PROGRESS           // Sign-in in progress
statusCodes.PLAY_SERVICES_NOT_AVAILABLE  // Play Services unavailable
statusCodes.SIGN_IN_REQUIRED      // Sign-in required

// Custom error class
class GoogleSignInError extends Error {
  code: string;
}
```

## Migration from react-native-google-signin

This package was created to address specific production security requirements where updating to newer Google Identity Services was necessary. The `@react-native-google-signin/google-signin` library offers many advanced features and premium functionality that this package does not provide.

**Important Note**: If you're using the premium features of `@react-native-google-signin/google-signin`, you should continue using that library as it provides much more comprehensive functionality. This package focuses solely on basic Google Sign-In implementation.

### When to Consider This Package:

- You need to use the latest Google Identity Services for security compliance
- You're building with React Native's new architecture (Turbo Modules)
- You only need basic Google Sign-In functionality
- You want minimal dependencies with centralized version management

### Key Differences:

1. **Scope**: Focused only on basic sign-in (no premium features)
2. **Architecture**: Built specifically for Turbo Modules
3. **Dependencies**: Uses latest Google Identity Services
4. **Management**: Centralized dependency versioning

### Migration Steps (if needed):

1. **Uninstall the existing package**:
   ```bash
   npm uninstall @react-native-google-signin/google-signin
   ```

2. **Install this package**:
   ```bash
   npm install @novastera-oss/rn-google-signin
   ```

3. **Update imports**:
   ```typescript
   // Before
   import { GoogleSignin } from '@react-native-google-signin/google-signin';
   
   // After
   import GoogleSignIn from '@novastera-oss/rn-google-signin';
   ```

4. **Update method calls** (if using the old class-based API):
   ```typescript
   // Before
   GoogleSignin.configure(config);
   
   // After
   GoogleSignIn.configure(config);
   ```

The basic sign-in API is designed to be compatible to minimize code changes during migration.

## Troubleshooting

### Common Issues

#### iOS: "No such module 'GoogleSignIn'"

Make sure you've run:
```bash
cd ios && pod install
```

#### Android: "Module not found"

Ensure you've added the Google Services plugin to your `android/app/build.gradle`:
```gradle
apply plugin: 'com.google.gms.google-services'
```

#### "DEVELOPER_ERROR" on Android

- Verify your `google-services.json` is in the correct location
- Check that your package name matches the one in Google Console
- Ensure you've configured the SHA-1 fingerprint

#### "Sign in failed" on iOS

- Verify your `GoogleService-Info.plist` is added to your Xcode project
- Check that your URL scheme matches the `REVERSED_CLIENT_ID`
- Ensure the client ID in your config matches the one in the plist

### Debug Mode

Enable debug logging:

```typescript
// Add this for development
if (__DEV__) {
  console.log('Google Sign-In Debug Mode Enabled');
}
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Initialize submodules: `git submodule update --init --recursive`
3. Install dependencies: `yarn install`
4. Set up iOS dependencies: `npm run setup-ios-deps`
5. Run the example: `yarn example`

### Working with Submodules

This package uses Git submodules for iOS SDK dependencies. Here are some useful commands:

```bash
# Initialize submodules (first time)
git submodule update --init --recursive

# Update submodules to latest
git submodule update --remote

# Update to a specific commit (in the submodule directory)
cd ios/third-party/GoogleSignIn-iOS
git checkout <commit-hash>
cd -
git add ios/third-party/GoogleSignIn-iOS
git commit -m "Update GoogleSignIn to <commit-hash>"
```

### Version Management

This package uses a centralized dependency management system:

- **`dependencies.json`**: Contains all third-party dependency versions
- **`package.json`**: Contains the package version and React Native compatibility
- **Automatic synchronization**: Scripts and build files read from these sources

To update dependency versions:

1. **Update GoogleSignIn iOS SDK**:
   ```bash
   # Edit dependencies.json
   # Update version and commit hash
   npm run setup-ios-deps
   ```

2. **Update Android dependencies**:
   ```bash
   # Edit dependencies.json under dependencies.googleSignIn.android
   # Gradle will automatically use new versions
   ```

3. **Update package version**:
   ```bash
   # Edit package.json version field
   # Plugin and scripts will automatically use new version
   ```

This ensures all parts of the package stay synchronized and reduces maintenance overhead.

### Testing

```bash
yarn test
yarn typecheck
yarn lint
```

## License

Apache 2.0 © [Novastera](https://novastera.com)
