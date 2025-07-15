const path = require('path');

const project = (() => {
  try {
    const { configureProjects } = require('react-native-test-app');
    return configureProjects({
      android: {
        sourceDir: path.join('example', 'android'),
      },
      ios: {
        sourceDir: path.join('example', 'ios'),
      },
    });
  } catch (_) {
    return undefined;
  }
})();

module.exports = {
  dependencies: {
    // Help rn-cli find and autolink this library with consistent naming
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
    ...(project
      ? {
          expo: {
            // otherwise RN cli will try to autolink expo
            platforms: {
              ios: null,
              android: null,
            },
          },
        }
      : undefined),
  },
  ...(project ? { project } : undefined),
}; 