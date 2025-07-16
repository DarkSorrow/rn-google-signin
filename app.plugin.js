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

// Import the compiled expo plugin
let withRNGoogleSignin;
try {
  // Try to load the compiled version first
  withRNGoogleSignin = require('./lib/typescript/expo-plugin/index.js').default;
} catch {
  // Fallback to the TypeScript version if compiled version doesn't exist
  withRNGoogleSignin = require('./expo-plugin/index.ts').default;
}

module.exports = createRunOncePlugin(withRNGoogleSignin, 'rn-google-signin-plugin', getPackageVersion()); 