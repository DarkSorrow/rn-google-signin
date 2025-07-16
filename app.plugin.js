const withRNGoogleSignin = require('./expo-plugin/index').default;

module.exports = function withPlugin(config) {
  return withRNGoogleSignin(config);
}; 