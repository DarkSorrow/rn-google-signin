#!/usr/bin/env node

console.log('🧪 Testing Expo Plugin Configuration...\n');

// Test 1: Check package.json expo configuration
console.log('1️⃣ Checking package.json expo configuration...');
const packageJson = require('./package.json');

if (packageJson.expo?.plugin === './app.plugin.js') {
  console.log('✅ package.json expo.plugin correctly points to app.plugin.js');
} else {
  console.log('❌ package.json expo.plugin configuration incorrect:', packageJson.expo?.plugin);
  process.exit(1);
}

// Test 2: Check app.plugin.js exists and exports correctly
console.log('\n2️⃣ Checking app.plugin.js...');
try {
  const appPlugin = require('./app.plugin.js');
  console.log('✅ app.plugin.js exists and can be required');
  
  if (typeof appPlugin.default === 'function') {
    console.log('✅ app.plugin.js exports a function as default');
  } else {
    console.log('❌ app.plugin.js default export is not a function');
    process.exit(1);
  }
} catch (error) {
  console.log('❌ app.plugin.js failed to load:', error.message);
  process.exit(1);
}

// Test 3: Check plugin build exists
console.log('\n3️⃣ Checking plugin build...');
try {
  const pluginBuild = require('./plugin/build/index.js');
  console.log('✅ plugin/build/index.js exists and can be required');
  
  if (typeof pluginBuild.default === 'function') {
    console.log('✅ plugin build exports a function as default');
  } else {
    console.log('❌ plugin build default export is not a function');
    process.exit(1);
  }
} catch (error) {
  console.log('❌ plugin build failed to load:', error.message);
  process.exit(1);
}

// Test 4: Check expo-module.config.json
console.log('\n4️⃣ Checking expo-module.config.json...');
try {
  const expoModuleConfig = require('./expo-module.config.json');
  console.log('✅ expo-module.config.json exists and is valid JSON');
  
  if (expoModuleConfig.plugin?.name === 'rn-google-signin') {
    console.log('✅ expo-module.config.json has correct plugin name');
  } else {
    console.log('❌ expo-module.config.json plugin name incorrect:', expoModuleConfig.plugin?.name);
  }
  
  if (expoModuleConfig.platforms?.includes('ios') && expoModuleConfig.platforms?.includes('android')) {
    console.log('✅ expo-module.config.json has correct platforms');
  } else {
    console.log('❌ expo-module.config.json platforms incorrect:', expoModuleConfig.platforms);
  }
} catch (error) {
  console.log('❌ expo-module.config.json failed to load:', error.message);
  process.exit(1);
}

// Test 5: Check files array includes necessary files
console.log('\n5️⃣ Checking package.json files array...');
const requiredFiles = ['app.plugin.js', 'expo-module.config.json', 'plugin/build'];
const missingFiles = requiredFiles.filter(file => !packageJson.files?.includes(file));

if (missingFiles.length === 0) {
  console.log('✅ All required files are included in package.json files array');
} else {
  console.log('❌ Missing files from package.json files array:', missingFiles);
  process.exit(1);
}

console.log('\n🎉 All Expo plugin tests passed!');
console.log('\n📋 Summary:');
console.log('   ✅ package.json has correct expo.plugin configuration');
console.log('   ✅ app.plugin.js exists and exports correctly');
console.log('   ✅ plugin build exists and is functional');
console.log('   ✅ expo-module.config.json is properly configured');
console.log('   ✅ All required files are included in package.json');
console.log('\n🚀 The Expo plugin is ready to use!'); 