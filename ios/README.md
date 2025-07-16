# iOS Implementation

This directory contains the iOS implementation of the Google Sign-In Turbo Module.

## Files

- `RnGoogleSignin.swift` - Main Swift implementation of the Turbo Module
- `RnGoogleSignin.h` - Header file declaring the module interface
- `RnGoogleSignin-Bridging-Header.h` - Bridging header for Swift-Objective-C interoperability
- `module.modulemap` - Module map for exposing Swift module to Objective-C

## Implementation Details

### Swift Implementation
The main implementation is written in Swift to take advantage of the modern Google Sign-In SDK and better type safety. The implementation includes:

- **Configuration**: Sets up Google Sign-In with client IDs and optional parameters
- **Sign In**: Handles both interactive and silent sign-in flows
- **Sign Out**: Manages user sign-out and access revocation
- **User State**: Provides methods to check sign-in status and get current user
- **Token Management**: Handles access token and ID token retrieval

### Key Features

1. **Turbo Module Compliance**: Implements the `NativeRnGoogleSigninSpec` interface
2. **Promise-based API**: All async operations return promises
3. **Error Handling**: Comprehensive error handling with meaningful error messages
4. **Configuration Flexibility**: Supports multiple ways to provide client IDs
5. **Scope Management**: Handles default scopes and additional scope requests

### Dependencies

- GoogleSignIn ~> 9.0
- GoogleSignInSwiftSupport ~> 9.0

### Configuration

The module can be configured with:
- `iosClientId`: iOS-specific client ID
- `webClientId`: Web client ID for server-side operations
- `scopes`: Array of OAuth scopes
- `hostedDomain`: Optional hosted domain restriction
- `googleServicePlistPath`: Path to GoogleService-Info.plist

### Usage

```swift
// Configure the module
RnGoogleSignin.configure([
    "iosClientId": "your-ios-client-id",
    "webClientId": "your-web-client-id",
    "scopes": ["profile", "email"]
])

// Sign in
let result = await RnGoogleSignin.signIn()

// Check sign-in status
let isSignedIn = await RnGoogleSignin.isSignedIn()

// Get current user
let user = await RnGoogleSignin.getCurrentUser()

// Sign out
await RnGoogleSignin.signOut()
```

## Building

The module uses CocoaPods for dependency management. The `RnGoogleSignin.podspec` file configures:

- Swift bridging header
- Module map for Swift-Objective-C interoperability
- Google Sign-In dependencies
- Source file inclusion

## Notes

- The implementation uses the latest Google Sign-In SDK (version 9.0.0)
- All UI operations are performed on the main thread
- Error handling follows Google Sign-In SDK patterns
- The module is designed to be backward compatible with the original react-native-google-signin API 