import {
  AndroidConfig,
  IOSConfig,
  createRunOncePlugin,
  withInfoPlist,
  withPlugins,
} from '@expo/config-plugins';
import type { ConfigPlugin } from '@expo/config-plugins';
import { readFileSync, existsSync } from 'fs';
import { join } from 'path';
// We'll parse plist manually to avoid dependency issues

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

const withGoogleSignin: ConfigPlugin<GoogleSigninOptions | void> = (config) => {
  return withPlugins(config, [
    // iOS configuration - automatically reads GoogleService-Info.plist and configures
    withGoogleSigniniOS,
    // Android configuration - automatically handles google-services.json
    withGoogleSigninAndroid,
  ]);
};

const withGoogleSigniniOS: ConfigPlugin = (config) => {
  return withPlugins(config, [
    // First handle the GoogleService-Info.plist file
    IOSConfig.Google.withGoogleServicesFile,
    // Then extract and configure the client IDs
    withGoogleSigninInfoPlist,
  ]);
};

const withGoogleSigninInfoPlist: ConfigPlugin = (config) => {
  return withInfoPlist(config, (config) => {
    const projectRoot = config.modRequest?.projectRoot || process.cwd();
    const googleServicesPlistPath = join(projectRoot, 'ios', 'GoogleService-Info.plist');
    
    if (existsSync(googleServicesPlistPath)) {
      try {
        const googleServicesContent = readFileSync(googleServicesPlistPath, 'utf8');
        // Simple plist parsing for required fields
        const clientIdMatch = googleServicesContent.match(/<key>CLIENT_ID<\/key>\s*<string>([^<]+)<\/string>/);
        const reversedClientIdMatch = googleServicesContent.match(/<key>REVERSED_CLIENT_ID<\/key>\s*<string>([^<]+)<\/string>/);
        const bundleIdMatch = googleServicesContent.match(/<key>BUNDLE_ID<\/key>\s*<string>([^<]+)<\/string>/);
        
        const googleServicesPlist = {
          CLIENT_ID: clientIdMatch?.[1],
          REVERSED_CLIENT_ID: reversedClientIdMatch?.[1],
          BUNDLE_ID: bundleIdMatch?.[1]
        };
        
        const clientId = googleServicesPlist.CLIENT_ID;
        const reversedClientId = googleServicesPlist.REVERSED_CLIENT_ID;
        const bundleId = config.ios?.bundleIdentifier || googleServicesPlist.BUNDLE_ID;
        
        if (clientId) {
          // Add GIDClientID to Info.plist
          config.modResults.GIDClientID = clientId;
          
          // Add URL schemes for Google Sign-In
          const existingURLTypes = config.modResults.CFBundleURLTypes || [];
          const newURLTypes = [...existingURLTypes];
          
          // Add reversed client ID scheme (required for Google Sign-In)
          if (reversedClientId) {
            const reversedClientExists = newURLTypes.some((urlType: any) => 
              urlType.CFBundleURLSchemes?.includes(reversedClientId)
            );
            
            if (!reversedClientExists) {
              newURLTypes.push({
                CFBundleURLName: 'GoogleSignIn',
                CFBundleURLSchemes: [reversedClientId]
              });
            }
          }
          
          // Add bundle ID scheme (for deep linking)
          if (bundleId) {
            const bundleIdExists = newURLTypes.some((urlType: any) => 
              urlType.CFBundleURLSchemes?.includes(bundleId)
            );
            
            if (!bundleIdExists) {
              newURLTypes.push({
                CFBundleURLName: 'BundleID',
                CFBundleURLSchemes: [bundleId]
              });
            }
          }
          
          config.modResults.CFBundleURLTypes = newURLTypes;
        }
      } catch (error) {
        console.warn('Failed to read GoogleService-Info.plist:', error);
      }
    } else {
      console.warn('GoogleService-Info.plist not found. Please add it to your ios/ directory.');
    }
    
    return config;
  });
};

const withGoogleSigninAndroid: ConfigPlugin = (config) => {
  return withPlugins(config, [
    // Automatically handle google-services.json and gradle configuration
    AndroidConfig.GoogleServices.withClassPath,
    AndroidConfig.GoogleServices.withApplyPlugin,
    AndroidConfig.GoogleServices.withGoogleServicesFile,
  ]);
};

export default createRunOncePlugin(withGoogleSignin, GOOGLE_SIGNIN_PLUGIN, getPackageVersion()); 