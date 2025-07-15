// Minimal ESLint v9 configuration - only lint JS files, let TypeScript handle TS files
module.exports = [
  {
    ignores: [
      'node_modules/**',
      'lib/**', 
      'coverage/**',
      'ios/build/**',
      'android/build/**',
      'example/**',
      'plugin/build/**',
      '**/*.ts',
      '**/*.tsx'
    ]
  },
  {
    files: ['**/*.js'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module'
    },
    rules: {
      'no-unused-vars': 'warn',
      'no-console': 'off'
    }
  }
]; 