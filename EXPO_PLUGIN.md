# Expo Plugin Integration

This document explains how the Expo plugin works and how to use it in your Expo projects.

## Overview

The `@novastera-oss/rn-google-signin` module includes an Expo config plugin that automatically handles the configuration required for Google Sign-In on both iOS and Android platforms.

## What the Plugin Does

### iOS Configuration

1. **Reads GoogleService-Info.plist**: Automatically reads the `ios/GoogleService-Info.plist` file
2. **Configures Info.plist**: Adds required entries to your app's `Info.plist`:
   - `GIDClientID`: The Google client ID for iOS
   - `CFBundleURLTypes`: URL schemes for Google Sign-In and deep linking

### Android Configuration

1. **Handles google-services.json**: Automatically processes the `android/app/google-services.json` file
2. **Adds Required Metadata**: Adds the necessary metadata for Google Sign-In:
   - `com.google.android.gms.auth.api.fallback`: Required for Google Sign-In fallback

## Installation

### For Expo SDK 49+

```bash
npx expo install @novastera-oss/rn-google-signin
```

### For Expo SDK 48 and below

```bash
npm install @novastera-oss/rn-google-signin
# or
yarn add @novastera-oss/rn-google-signin
```

## Configuration

### Automatic Configuration (Recommended)

The plugin works automatically when you add it to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": [
      "@novastera-oss/rn-google-signin"
    ]
  }
}
```

### Manual Configuration

If you need to specify custom client IDs:

```json
{
  "expo": {
    "plugins": [
      ["@novastera-oss/rn-google-signin", {
        "iosClientId": "your-ios-client-id.apps.googleusercontent.com",
        "androidClientId": "your-android-client-id.apps.googleusercontent.com",
        "webClientId": "your-web-client-id.apps.googleusercontent.com"
      }]
    ]
  }
}
```

## Required Files

### iOS

Place your `GoogleService-Info.plist` file in the `ios/` directory of your Expo project:

```
your-expo-project/
├── ios/
│   └── GoogleService-Info.plist  # Required for iOS
├── app.json
└── ...
```

### Android

Place your `google-services.json` file in the `android/app/` directory:

```
your-expo-project/
├── android/
│   └── app/
│       └── google-services.json  # Required for Android
├── app.json
└── ...
```

## Plugin Options

| Option | Type | Description |
|--------|------|-------------|
| `iosClientId` | string | Custom iOS client ID (optional) |
| `androidClientId` | string | Custom Android client ID (optional) |
| `webClientId` | string | Custom web client ID (optional) |
| `iosReversedClientId` | string | Custom iOS reversed client ID (optional) |

## Troubleshooting

### Plugin Not Working

1. **Check File Locations**: Ensure `GoogleService-Info.plist` and `google-services.json` are in the correct directories
2. **Rebuild**: Run `npx expo prebuild --clean` to rebuild with the plugin
3. **Check Logs**: Look for plugin warnings in the build output

### Common Issues

#### "GoogleService-Info.plist not found"

**Solution**: Add your `GoogleService-Info.plist` file to the `ios/` directory of your Expo project.

#### "google-services.json not found"

**Solution**: Add your `google-services.json` file to the `android/app/` directory of your Expo project.

#### Plugin Configuration Errors

**Solution**: Ensure your `app.json` has the correct plugin configuration:

```json
{
  "expo": {
    "plugins": [
      "@novastera-oss/rn-google-signin"
    ]
  }
}
```

## Development

### Testing the Plugin

To test the plugin locally:

1. Build the module: `yarn prepare`
2. Link it in your Expo project
3. Run `npx expo prebuild` to apply the plugin

### Plugin Source

The plugin source is located at `src/withRNGoogleSignin.ts` and exposes the configuration through `app.plugin.js`.

## Migration from Manual Configuration

If you were previously configuring Google Sign-In manually, you can now remove:

### iOS (Info.plist)
- Manual `GIDClientID` entries
- Manual `CFBundleURLTypes` for Google Sign-In

### Android (AndroidManifest.xml)
- Manual `google-services.json` handling
- Manual metadata entries for Google Sign-In

The plugin handles all of these automatically. 