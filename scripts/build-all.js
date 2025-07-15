#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('🚀 Building @novastera-oss/rn-google-signin...');

// Clean everything first
console.log('🧹 Cleaning previous builds...');
try {
  execSync('npm run clean', { stdio: 'inherit' });
} catch (e) {
  console.log('Clean failed, continuing...');
}

// Build main TypeScript files
console.log('📦 Building TypeScript...');
try {
  execSync('npx tsc -p tsconfig.build.json', { stdio: 'inherit' });
  console.log('✅ TypeScript build complete');
} catch (e) {
  console.error('❌ TypeScript build failed');
  process.exit(1);
}

// Build plugin
console.log('🔧 Building Expo plugin...');
try {
  execSync('npx tsc -p plugin/tsconfig.json', { stdio: 'inherit' });
  console.log('✅ Plugin build complete');
} catch (e) {
  console.error('❌ Plugin build failed');
  process.exit(1);
}

// Verify builds
console.log('🔍 Verifying builds...');

const requiredFiles = [
  'lib/index.js',
  'lib/index.d.ts',
  'lib/GoogleSignin.js',
  'lib/GoogleSignin.d.ts',
  'plugin/build/index.js',
  'plugin/build/withGoogleSignin.js'
];

let allGood = true;
for (const file of requiredFiles) {
  if (!fs.existsSync(file)) {
    console.error(`❌ Missing: ${file}`);
    allGood = false;
  } else {
    console.log(`✅ Found: ${file}`);
  }
}

if (allGood) {
  console.log('🎉 All builds successful!');
} else {
  console.error('❌ Some builds are missing');
  process.exit(1);
} 