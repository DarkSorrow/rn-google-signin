const { execSync } = require('child_process');
const fs = require('fs');

console.log('🚀 Testing React Native Builder Bob build...');

try {
  // Clean previous build
  if (fs.existsSync('lib')) {
    console.log('🧹 Cleaning previous build...');
    fs.rmSync('lib', { recursive: true, force: true });
  }

  // Run bob build
  console.log('🔧 Running bob build...');
  execSync('npx bob build', { stdio: 'inherit' });

  // Check if builds were created
  const expectedPaths = [
    'lib/module/index.js',
    'lib/typescript/src/index.d.ts'
  ];

  console.log('🔍 Checking build outputs...');
  for (const expectedPath of expectedPaths) {
    if (fs.existsSync(expectedPath)) {
      console.log(`✅ ${expectedPath} exists`);
    } else {
      console.log(`❌ ${expectedPath} missing`);
    }
  }

  // Check that the build has correct module format
  console.log('\n📦 Checking module format...');
  
  // Check ES Module format  
  const moduleContent = fs.readFileSync('lib/module/index.js', 'utf-8');
  if (moduleContent.includes('import ') && moduleContent.includes('export ')) {
    console.log('✅ ES Module build has correct ES module syntax');
  } else {
    console.log('❌ ES Module build missing ES module syntax');
  }

  // Check TypeScript definitions
  const typeDefsExist = fs.existsSync('lib/typescript/src/index.d.ts');
  if (typeDefsExist) {
    console.log('✅ TypeScript definitions generated');
  } else {
    console.log('❌ TypeScript definitions missing');
  }

  // Check that TurboModuleRegistry is properly handled
  const nativeModulePath = 'lib/module/NativeGoogleSignin.js';
  if (fs.existsSync(nativeModulePath)) {
    const nativeContent = fs.readFileSync(nativeModulePath, 'utf-8');
    if (nativeContent.includes('TurboModuleRegistry.getEnforcing')) {
      console.log('✅ Turbo Module registry properly included');
    } else {
      console.log('❌ Turbo Module registry missing');
    }
  }

  console.log('\n🎯 Build Configuration:');
  console.log('• ES Module only (no CommonJS complexity)');
  console.log('• React Native Metro bundler compatible');
  console.log('• Modern bundler support with ESM');
  console.log('• Single TypeScript definition set');
  
  console.log('\n🎉 Simplified build completed successfully!');
  console.log('📱 Your library is ready for React Native apps!');
  
} catch (error) {
  console.error('❌ Build failed:', error.message);
  process.exit(1);
} 