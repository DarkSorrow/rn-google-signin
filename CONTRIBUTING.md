# Contributing to @novastera/rn-google-signin

Thank you for your interest in contributing to this React Native Google Sign-In package! We welcome contributions from the community.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Working with Dependencies](#working-with-dependencies)
- [Release Process](#release-process)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct:

- Be respectful and inclusive
- Focus on constructive feedback
- Help maintain a welcoming environment for all contributors
- Respect that this package focuses on basic Google Sign-In functionality

## Getting Started

### Prerequisites

- **Node.js** 22+ and npm
- **React Native development environment** (Android Studio, Xcode)
- **Git** with submodule support
- **Platform-specific tools**:
  - **macOS**: Xcode 15+, CocoaPods
  - **Windows**: Android Studio, PowerShell 5.1+
  - **Linux**: Android Studio

### First-time Setup

1. **Fork and clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/rn-google-signin.git
   cd rn-google-signin
   ```

2. **Initialize Git submodules**:
   ```bash
   git submodule update --init --recursive
   ```

3. **Install dependencies**:
   ```bash
   npm install
   ```

4. **Set up iOS dependencies** (macOS/Linux):
   ```bash
   npm run setup-ios-deps
   ```
   
   **Windows**:
   ```bash
   npm run setup-ios-deps:windows
   ```

## Development Setup

### Project Structure

```
rn-google-signin/
├── src/                          # TypeScript source code
│   ├── GoogleSignin.ts          # Main API wrapper
│   ├── NativeGoogleSignin.ts    # Turbo Module specification
│   ├── types.ts                 # TypeScript type definitions
│   └── index.ts                 # Public API exports
├── android/                      # Android native implementation
│   ├── src/main/java/...        # Kotlin source files
│   └── build.gradle             # Android build configuration
├── ios/                         # iOS native implementation
│   ├── *.swift, *.m, *.h       # Swift/Objective-C source files
│   └── third-party/             # Git submodules for iOS dependencies
├── plugin/                      # Expo config plugin
│   ├── src/withGoogleSignin.ts  # Plugin implementation
│   └── tsconfig.json           # Plugin TypeScript config
├── scripts/                     # Setup and utility scripts
├── lib/                         # Built output (auto-generated)
├── dependencies.json            # Centralized dependency versions
├── package.json                # Package configuration
└── rn-google-signin.podspec    # iOS CocoaPods specification
```

### Key Files

- **`src/GoogleSignin.ts`**: Main API implementation
- **`src/NativeGoogleSignin.ts`**: Turbo Module interface
- **`android/src/main/java/.../RNGoogleSigninModule.kt`**: Android implementation
- **`ios/RNGoogleSignin.swift`**: iOS implementation
- **`dependencies.json`**: Single source of truth for all dependency versions
- **`plugin/src/withGoogleSignin.ts`**: Expo config plugin

## Making Changes

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-bug-fix
```

### 2. Development Guidelines

#### TypeScript Code
- Use strict TypeScript (no `any` types)
- Follow the existing code style
- Add proper JSDoc comments for public APIs
- Ensure all exports have proper type definitions

#### Native Code (iOS)
- Follow Swift/Objective-C best practices
- Add proper error handling
- Use the existing patterns for Promise resolution/rejection
- Test on multiple iOS versions when possible

#### Native Code (Android)
- Follow Kotlin best practices
- Use coroutines for async operations
- Implement proper error handling
- Test on multiple Android API levels when possible

#### Expo Plugin
- Follow Expo plugin conventions
- Handle configuration edge cases gracefully
- Provide clear error messages

### 3. Code Style

Run the linter before committing:
```bash
npm run lint
npm run typecheck
```

Fix auto-fixable issues:
```bash
npm run lint:fix
```

## Testing

### Run Tests

```bash
# Run all tests
npm test

# Run TypeScript type checking
npm run typecheck

# Run linting
npm run lint
```

### Manual Testing

1. **Test iOS setup**:
   ```bash
   npm run setup-ios-deps
   # Verify ios/third-party/GoogleSignIn-iOS exists
   ```

2. **Test Android build** (if you have Android development setup):
   ```bash
   cd android && ./gradlew build
   ```

3. **Test plugin build**:
   ```bash
   npm run build:plugin
   # Verify plugin/build/ directory is created
   ```

### Integration Testing

If you have access to a React Native app:

1. Create a test app or use the example (if available)
2. Install your local version:
   ```bash
   npm pack
   # Install the generated .tgz file in your test app
   ```
3. Test the sign-in flow on both platforms

## Submitting Changes

### 1. Commit Guidelines

Use conventional commit messages:
```bash
git commit -m "feat: add silent sign-in support"
git commit -m "fix: resolve Android credential manager issue"
git commit -m "docs: update installation instructions"
git commit -m "refactor: simplify error handling"
```

### 2. Pull Request Process

1. **Push your branch**:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create a Pull Request** with:
   - Clear title and description
   - Reference any related issues
   - Include testing information
   - Note any breaking changes

3. **PR Requirements**:
   - All tests must pass
   - Code must be linted
   - TypeScript must compile without errors
   - Include relevant documentation updates

### 3. Review Process

- Maintainers will review your PR
- Address any requested changes
- Once approved, your PR will be merged

## Working with Dependencies

### Centralized Version Management

This project uses centralized dependency management via `dependencies.json`:

```json
{
  "dependencies": {
    "googleSignIn": {
      "ios": {
        "version": "9.0.0",
        "commit": "3996d908c7b3ce8a87d39c808f9a6b2a08fbe043"
      },
      "android": {
        "playServicesAuth": "20.7.0",
        "credentials": "1.2.2",
        "googleId": "1.1.0"
      }
    }
  }
}
```

### Updating Dependencies

#### iOS GoogleSignIn SDK

1. **Update `dependencies.json`**:
   ```json
   {
     "ios": {
       "version": "NEW_VERSION",
       "commit": "NEW_COMMIT_HASH"
     }
   }
   ```

2. **Update the submodule**:
   ```bash
   cd ios/third-party/GoogleSignIn-iOS
   git fetch
   git checkout NEW_COMMIT_HASH
   cd ../../..
   git add ios/third-party/GoogleSignIn-iOS
   git commit -m "deps: update GoogleSignIn iOS to NEW_VERSION"
   ```

#### Android Dependencies

1. **Update `dependencies.json`**:
   ```json
   {
     "android": {
       "playServicesAuth": "NEW_VERSION",
       "credentials": "NEW_VERSION",
       "googleId": "NEW_VERSION"
     }
   }
   ```

2. **Test the build**:
   ```bash
   cd android && ./gradlew build
   ```

### Git Submodules

Useful commands for working with submodules:

```bash
# Initialize submodules (first time)
git submodule update --init --recursive

# Update submodules to latest from remote
git submodule update --remote

# Update to specific commit
cd ios/third-party/GoogleSignIn-iOS
git checkout COMMIT_HASH
cd ../../..
git add ios/third-party/GoogleSignIn-iOS
git commit -m "Update GoogleSignIn submodule"

# Check submodule status
git submodule status
```

## Release Process

> **Note**: Only maintainers can create releases.

The release process is automated via GitHub Actions:

1. **Create a GitHub Release**:
   - Go to GitHub → Releases → "Draft a new release"
   - Tag: `v1.0.0` (follow semantic versioning)
   - Title: `Release 1.0.0`
   - Write release notes
   - Click "Publish release"

2. **Automated Process**:
   - GitHub Actions will automatically run CI tests
   - If tests pass, the package will be published to NPM
   - The release will be available on GitHub and NPM

## Questions or Issues?

- **Bug reports**: Create an issue with reproduction steps
- **Feature requests**: Create an issue with detailed description
- **Questions**: Start a discussion or create an issue
- **Security issues**: Email contact@novastera.com directly

## Thank You!

Your contributions help make this package better for everyone. We appreciate your time and effort in improving React Native Google Sign-In functionality. 
