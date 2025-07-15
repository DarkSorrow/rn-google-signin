import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
  withAndroidManifest,
  withInfoPlist,
  withPlugins,
} from '@expo/config-plugins';
import { readFileSync } from 'fs';
import { join } from 'path';

export interface GoogleSigninOptions {
  iosClientId?: string;
  androidClientId?: string;
  webClientId?: string;
  iosReversedClientId?: string;
}

// Get package version dynamically
function getPackageVersion(): string {
  try {
    const packageJsonPath = join(__dirname, '../../../package.json');
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    return packageJson.version || '1.0.0';
  } catch {
    return '1.0.0';
  }
}

const GOOGLE_SIGNIN_PLUGIN = 'rn-google-signin-plugin';

const withGoogleSignin: ConfigPlugin<GoogleSigninOptions | void> = (config, options) => {
  const {
    iosClientId,
    androidClientId,
    webClientId,
    iosReversedClientId,
  } = options || {};

  return withPlugins(config, [
    // Configure iOS
    [withGoogleSigniniOS, { iosClientId, iosReversedClientId }],
    // Configure Android
    [withGoogleSigninAndroid, { androidClientId, webClientId }],
  ]);
};

const withGoogleSigniniOS: ConfigPlugin<{
  iosClientId?: string;
  iosReversedClientId?: string;
}> = (config, { iosClientId, iosReversedClientId }) => {
  return withInfoPlist(config, (config) => {
    if (!iosClientId) {
      throw new Error(
        'Google Sign-In iOS client ID is required. Please provide it in the plugin options.'
      );
    }

    // Add URL scheme for Google Sign-In
    const reversedClientId = iosReversedClientId || iosClientId.split('.').reverse().join('.');
    
    if (!config.modResults.CFBundleURLTypes) {
      config.modResults.CFBundleURLTypes = [];
    }

    const urlTypes = config.modResults.CFBundleURLTypes;
    
    // Remove existing Google Sign-In URL scheme if it exists
    const filteredUrlTypes = urlTypes.filter(
      (urlType) => 
        !urlType.CFBundleURLSchemes?.includes(reversedClientId)
    );

    // Add the new URL scheme
    filteredUrlTypes.push({
      CFBundleURLName: 'GoogleSignIn',
      CFBundleURLSchemes: [reversedClientId],
    });

    config.modResults.CFBundleURLTypes = filteredUrlTypes;

    return config;
  });
};

const withGoogleSigninAndroid: ConfigPlugin<{
  androidClientId?: string;
  webClientId?: string;
}> = (config, { androidClientId, webClientId }) => {
  return withAndroidManifest(config, (config) => {
    const clientId = androidClientId || webClientId;
    
    if (!clientId) {
      throw new Error(
        'Google Sign-In Android client ID or web client ID is required. Please provide it in the plugin options.'
      );
    }

    // Add internet permission if not already present
    AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);
    
    // The permissions are typically handled automatically by the Google Play Services
    // but we can add specific configurations here if needed
    
    return config;
  });
};

export default createRunOncePlugin(withGoogleSignin, GOOGLE_SIGNIN_PLUGIN, getPackageVersion()); 