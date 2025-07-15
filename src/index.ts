import GoogleSignIn from './GoogleSignIn';
import { statusCodes, GoogleSignInError } from './types';
import type { GoogleSignInUser, GoogleSignInConfig, GoogleSignInTokens } from './types';

// Export the singleton instance as the default export
export default GoogleSignIn;

// Export all other members
export { 
  GoogleSignIn,
  statusCodes, 
  GoogleSignInError 
};

// Export types
export type { 
  GoogleSignInUser, 
  GoogleSignInConfig, 
  GoogleSignInTokens 
};
