# iOS Implementation

This directory contains the iOS implementation of the Google Sign-In Turbo Module.

## Files

- `RnGoogleSignin.mm` - Objective-C++ implementation of the Turbo Module
- `RnGoogleSignin.h` - Header declaring the module against the generated `NativeRnGoogleSigninSpec`
- `module.modulemap` - Module map for the pod

## Implementation Details

The module is written in Objective-C++ so it can implement the codegen'd TurboModule spec directly, without a Swift bridging layer. It covers:

- **Configuration**: Sets up `GIDSignIn` with `iosClientId`, `webClientId`, or the `CLIENT_ID` from `GoogleService-Info.plist`
- **Sign In**: Interactive sign-in (with optional nonce, hint and additional scopes) and silent restore of a previous session
- **Sign Out**: Sign-out and access revocation (`disconnect`)
- **User State**: Sign-in status and current user
- **Token Management**: Access/ID token retrieval with automatic refresh

### Threading

All module methods run on the main queue (`methodQueue`), because `GIDSignIn` presents UI and its completions are delivered on the main queue.

### User identifier

`user.id` is `GIDGoogleUser.userID`, the stable Google account ID (the OpenID `sub` claim). Android returns the same value.

### Dependencies

- GoogleSignIn ~> 10.1 (requires iOS 15.0+; AppAuth 3, GTMAppAuth 6, AppCheckCore 11/12)

GoogleSignIn 10.1.0 is the last release Google publishes to CocoaPods (trunk becomes read-only on December 2, 2026). Later releases will be Swift Package Manager only.

## Building

The module uses CocoaPods. `RnGoogleSignin.podspec` builds it as a static framework and depends on the official `GoogleSignIn` pod.

When the app builds pods as static libraries (the React Native / Expo default), `GoogleUtilities`, `RecaptchaInterop` and `AppCheckCore` need `:modular_headers => true` in the app's Podfile. The Expo config plugin adds these automatically; see the main README for bare React Native apps.
