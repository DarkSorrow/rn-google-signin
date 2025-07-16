// Expo Plugin for React Native Google Sign-In
// This is completely separate from the main TurboModule

import { appendScheme } from '@expo/config-plugins/build/ios/Scheme';
import {
  AndroidConfig,
  IOSConfig,
  createRunOncePlugin,
  withInfoPlist,
  withPlugins,
  withAndroidManifest,
} from '@expo/config-plugins';
import type { ConfigPlugin } from '@expo/config-plugins';
import { readFileSync } from 'fs';
import { join } from 'path';

// Plugin options interface - for manual configuration
export interface GoogleSigninOptions {
  iosUrlScheme?: string; // For manual setup without Firebase
}

// Get package version dynamically
function getPackageVersion(): string {
  try {
    const packageJsonPath = join(__dirname, '../../package.json');
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    return packageJson.version || '1.0.0';
  } catch {
    return '1.0.0';
  }
}

const GOOGLE_SIGNIN_PLUGIN = 'rn-google-signin-plugin';

// Validate options for manual setup
function validateOptions(options: GoogleSigninOptions) {
  const messagePrefix = `rn-google-signin config plugin`;
  if (!options?.iosUrlScheme) {
    throw new Error(
      `${messagePrefix}: Missing \`iosUrlScheme\` in provided options: ${JSON.stringify(
        options,
      )}`,
    );
  }
  if (!options.iosUrlScheme.startsWith('com.googleusercontent.apps.')) {
    throw new Error(
      `${messagePrefix}: \`iosUrlScheme\` must start with "com.googleusercontent.apps": ${JSON.stringify(
        options,
      )}`,
    );
  }
}

// Plugin for manual setup (without Firebase)
const withGoogleSigninManual: ConfigPlugin<GoogleSigninOptions> = (config, options) => {
  validateOptions(options);
  return withPlugins(config, [
    // iOS - add URL scheme manually
    (cfg) => withGoogleUrlScheme(cfg, options),
    // Android - add required metadata
    withGoogleSigninAndroidManifest,
  ]);
};

// Add Google URL scheme to iOS Info.plist
const withGoogleUrlScheme: ConfigPlugin<GoogleSigninOptions> = (config, options) => {
  return withInfoPlist(config, (config) => {
    config.modResults = appendScheme(options.iosUrlScheme!, config.modResults);
    return config;
  });
};

// Plugin for automatic setup (with Firebase)
const withGoogleSigninAutomatic: ConfigPlugin = (config) => {
  return withPlugins(config, [
    // Android - handle google-services.json
    AndroidConfig.GoogleServices.withClassPath,
    AndroidConfig.GoogleServices.withApplyPlugin,
    AndroidConfig.GoogleServices.withGoogleServicesFile,
    
    // iOS - handle GoogleService-Info.plist
    IOSConfig.Google.withGoogleServicesFile,
    
    // Add required metadata for both platforms
    withGoogleSigninAndroidManifest,
  ]);
};

// Android manifest configuration
const withGoogleSigninAndroidManifest: ConfigPlugin = (config) => {
  return withAndroidManifest(config, (config) => {
    const application = config.modResults.manifest.application?.[0];
    
    if (application) {
      // Add required metadata for Google Sign-In
      const metaData = application['meta-data'] || [];
      
      // Add Google Sign-In fallback metadata
      const fallbackMetadata = {
        $: {
          'android:name': 'com.google.android.gms.auth.api.fallback',
          'android:value': 'true',
        },
      };
      
      // Check if metadata already exists
      const existingFallback = metaData.find((item: any) => 
        item.$?.['android:name'] === 'com.google.android.gms.auth.api.fallback'
      );
      
      if (!existingFallback) {
        metaData.push(fallbackMetadata);
      }
      
      application['meta-data'] = metaData;
    }
    
    return config;
  });
};

// Main plugin function - handles both automatic and manual modes
const withRNGoogleSignin: ConfigPlugin<GoogleSigninOptions | void> = (config, options) => {
  return options
    ? withGoogleSigninManual(config, options)
    : withGoogleSigninAutomatic(config);
};

// Export the plugin with run-once protection
export default createRunOncePlugin(withRNGoogleSignin, GOOGLE_SIGNIN_PLUGIN, getPackageVersion()); 