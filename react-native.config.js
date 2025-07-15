const path = require('path');

module.exports = {
  dependencies: {
    '@novastera-oss/rn-google-signin': {
      root: __dirname,
      platforms: {
        android: {
          sourceDir: path.join(__dirname, 'android'),
          packageImportPath: 'import com.novastera.rngooglesignin.RNGoogleSigninPackage;',
          packageInstance: 'new RNGoogleSigninPackage()',
        },
        ios: {
          sourceDir: path.join(__dirname, 'ios'),
          podspec: 'rn-google-signin.podspec',
        },
      },
    },
  },
}; 