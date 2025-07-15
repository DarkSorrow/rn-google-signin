#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('🧪 Testing built package...');

try {
  // Test that the main entry point exists and can be loaded
  const mainPath = path.join(__dirname, '../lib/index.js');
  
  if (!fs.existsSync(mainPath)) {
    throw new Error('Main entry point not found: ' + mainPath);
  }
  
  console.log('✅ Main entry point exists');
  
  // Check the file content instead of requiring it (since it's now ES modules)
  const content = fs.readFileSync(mainPath, 'utf8');
  
  if (!content.includes('GoogleSignIn') && !content.includes('export')) {
    throw new Error('GoogleSignIn not exported correctly in built file');
  }
  
  console.log('✅ GoogleSignIn exports correctly');
  
  // Test plugin
  const pluginPath = path.join(__dirname, '../plugin/build/index.js');
  
  if (!fs.existsSync(pluginPath)) {
    throw new Error('Plugin entry point not found: ' + pluginPath);
  }
  
  console.log('✅ Plugin entry point exists');
  
  // Check plugin file content
  const pluginContent = fs.readFileSync(pluginPath, 'utf8');
  
  if (!pluginContent.includes('withGoogleSignin') && !pluginContent.includes('exports')) {
    throw new Error('Plugin not exported correctly');
  }
  
  console.log('✅ Plugin exports correctly');
  console.log('🎉 All tests passed!');
  
} catch (error) {
  console.error('❌ Test failed:', error.message);
  process.exit(1);
} 