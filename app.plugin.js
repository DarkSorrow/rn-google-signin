const { createRunOncePlugin } = require('@expo/config-plugins');
const { readFileSync } = require('fs');
const { join } = require('path');

// Get package version dynamically
function getPackageVersion() {
  try {
    const packageJsonPath = join(__dirname, './package.json');
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    return packageJson.version || '1.0.0';
  } catch {
    return '1.0.0';
  }
}

// Simple plugin that doesn't interfere with the main TurboModule
const withRNGoogleSignin = (config) => {
  // This plugin is optional and only adds basic configuration
  // The main functionality comes from the TurboModule itself
  return config;
};

module.exports = createRunOncePlugin(withRNGoogleSignin, 'rn-google-signin-plugin', getPackageVersion()); 