console.log('Testing individual file imports to isolate the problem...\n');

// Test each file individually
const files = [
  './lib/types.js',
  './lib/NativeGoogleSignin.js', 
  './lib/GoogleSignIn.js',
  './lib/index.js'
];

for (const file of files) {
  try {
    console.log(`Testing ${file}...`);
    delete require.cache[require.resolve(file)]; // Clear cache
    const module = require(file);
    console.log(`✅ ${file} imported successfully`);
    console.log(`   Exports:`, Object.keys(module));
  } catch (error) {
    console.log(`❌ ${file} failed:`, error.message);
    if (error.message.includes('typeof')) {
      console.log(`   🎯 FOUND THE CULPRIT: ${file}`);
      
      // Read the file and look for the problematic line
      const fs = require('fs');
      const content = fs.readFileSync(require.resolve(file), 'utf8');
      const lines = content.split('\n');
      
      console.log('\n   File content analysis:');
      lines.forEach((line, index) => {
        if (line.includes('typeof')) {
          console.log(`   Line ${index + 1}: ${line.trim()}`);
        }
      });
      
      break; // Stop at first error
    }
  }
}

console.log('\nIf no culprit found above, the issue might be in a transitive dependency.'); 