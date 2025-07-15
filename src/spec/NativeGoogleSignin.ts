import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface GoogleSignInUser {
  user: {
    id: string;
    name: string | null;
    email: string;
    photo: string | null;
    familyName: string | null;
    givenName: string | null;
  };
  scopes: string[];
  serverAuthCode: string | null;
  idToken: string | null;
}

export interface GoogleSignInConfig {
  webClientId?: string;
  iosClientId?: string;
  androidClientId?: string;
  scopes?: string[];
  offlineAccess?: boolean;
  hostedDomain?: string;
  forceCodeForRefreshToken?: boolean;
  accountName?: string;
  googleServicePlistPath?: string;
}

export interface GoogleSignInTokens {
  accessToken: string;
  idToken: string | null;
}

export interface Spec extends TurboModule {
  // Configuration
  configure(config: GoogleSignInConfig): Promise<void>;
  
  // Sign in methods
  hasPlayServices(): Promise<boolean>;
  signIn(): Promise<GoogleSignInUser>;
  signInSilently(): Promise<GoogleSignInUser>;
  addScopes(scopes: string[]): Promise<GoogleSignInUser>;
  
  // Sign out methods
  signOut(): Promise<void>;
  revokeAccess(): Promise<void>;
  
  // User state
  isSignedIn(): Promise<boolean>;
  getCurrentUser(): Promise<GoogleSignInUser | null>;
  
  // Utilities
  clearCachedAccessToken(accessToken: string): Promise<void>;
  getTokens(): Promise<GoogleSignInTokens>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeGoogleSignin'); 