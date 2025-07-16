#!/usr/bin/env node

// Test script to verify the expo plugin can be loaded
console.log('Testing expo plugin...');

try {
  // Test 1: Try to load the built plugin directly
  console.log('1. Testing direct plugin load...');
  const plugin = require('./expo-plugin/build/withGoogleSignIn');
  console.log('✅ Plugin loaded successfully:', typeof plugin);
  
  // Test 2: Try to load via app.plugin.js
  console.log('2. Testing app.plugin.js...');
  const appPlugin = require('./app.plugin.js');
  console.log('✅ App plugin loaded successfully:', typeof appPlugin);
  
  // Test 3: Test with a proper mock config (Expo expects _internal.projectRoot)
  console.log('3. Testing plugin with proper mock config...');
  const mockConfig = {
    name: 'test-app',
    slug: 'test-app',
    version: '1.0.0',
    platforms: ['ios', 'android'],
    _internal: {
      projectRoot: process.cwd(),
      dynamicConfigPath: null,
      staticConfigPath: null,
      packageJsonPath: './package.json'
    }
  };
  
  const result = appPlugin(mockConfig, { iosUrlScheme: 'com.googleusercontent.apps.test' });
  console.log('✅ Plugin executed successfully:', result.name);
  
  console.log('\n🎉 All tests passed! Plugin is working correctly.');
  
} catch (error) {
  console.error('❌ Plugin test failed:', error.message);
  console.error('Stack:', error.stack);
  process.exit(1);
} 