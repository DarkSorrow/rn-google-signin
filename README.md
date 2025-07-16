# React Native Google Sign-In

Modern React Native Google Sign-In with Turbo Modules support and Expo plugin integration.

## Features

- ✅ **Turbo Module Support** - Built with the new React Native architecture
- ✅ **Expo Plugin Support** - Optional Expo config plugin for automatic configuration
- ✅ **TypeScript Support** - Full TypeScript definitions
- ✅ **iOS & Android Support** - Native implementations for both platforms

## Installation

### For React Native CLI projects:

```bash
npm install @novastera-oss/rn-google-signin
# or
yarn add @novastera-oss/rn-google-signin
```

### For Expo projects:

```bash
npx expo install @novastera-oss/rn-google-signin
```

## Expo Plugin Setup (Optional)

The module includes an optional Expo config plugin that automatically handles:

- **iOS**: Reads `GoogleService-Info.plist` and configures `Info.plist`
- **Android**: Handles `google-services.json` and adds required metadata

### Automatic Configuration

The plugin automatically:

1. **iOS Configuration**:
   - Reads `ios/GoogleService-Info.plist`
   - Adds `GIDClientID` to `Info.plist`
   - Configures URL schemes for Google Sign-In
   - Adds bundle ID scheme for deep linking

2. **Android Configuration**:
   - Handles `google-services.json` file
   - Adds required Google Sign-In metadata
   - Configures Gradle dependencies

### Manual Setup (if needed)

If you prefer manual configuration, you can disable the plugin and configure manually:

```json
{
  "expo": {
    "plugins": [
      ["@novastera-oss/rn-google-signin", {
        "iosClientId": "your-ios-client-id",
        "androidClientId": "your-android-client-id"
      }]
    ]
  }
}
```

## Usage

```typescript
import { multiply } from '@novastera-oss/rn-google-signin';

const result = multiply(2, 3); // Returns 6
```

## Development

### Prerequisites

- Node.js 18+
- React Native 0.79+
- TypeScript 5.8+

### Setup

```bash
# Install dependencies
yarn install

# Build the module
yarn prepare

# Run tests
yarn test

# Type checking
yarn typecheck
```

### Project Structure

```
├── src/                      # Pure TurboModule source
│   ├── index.tsx            # Main module exports
│   └── NativeRnGoogleSignin.ts # Turbo Module spec
├── expo-plugin/             # Optional Expo plugin (separate)
│   └── index.ts             # Expo config plugin
├── android/                 # Android native code
├── ios/                    # iOS native code
├── example/                # Example app
├── app.plugin.js          # Expo plugin entry point
└── expo-module.config.json # Expo autolinking config
```

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
