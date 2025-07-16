# Expo Plugin for React Native Google Sign-In

This directory contains the optional Expo config plugin for the React Native Google Sign-In module.

## Purpose

The Expo plugin automatically configures Google Sign-In for both iOS and Android platforms when used in Expo projects. It's completely separate from the main TurboModule to maintain clean architecture.

## Files

- `index.ts` - Main Expo config plugin

## Usage

The plugin is automatically available when you install `@novastera-oss/rn-google-signin` in an Expo project. No additional setup is required.

## Architecture

This plugin is designed to be:
- **Optional**: Only loaded when used in Expo projects
- **Independent**: Separate from the main TurboModule source
- **Clean**: No dependencies on the main module
- **Helper**: Only configures native files, doesn't interfere with the TurboModule

## What it does

The plugin supports two modes:

### Automatic Mode (with Firebase)
When no options are provided, the plugin automatically:
1. **iOS**: Reads `GoogleService-Info.plist` and configures `Info.plist`
2. **Android**: Handles `google-services.json` and adds required metadata
3. **No interference**: Doesn't touch the main TurboModule code

### Manual Mode (without Firebase)
When `iosUrlScheme` option is provided:
1. **iOS**: Adds the provided URL scheme to `Info.plist`
2. **Android**: Adds required metadata for Google Sign-In
3. **Validation**: Ensures the URL scheme follows Google's format

## Development

The plugin uses `@expo/config-plugins` as a peer dependency (defined in the main package.json) and is only loaded when needed by Expo's build system. 