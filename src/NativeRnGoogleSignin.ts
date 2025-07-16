import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface ConfigureParams {
  webClientId?: string;
  androidClientId?: string;
  iosClientId?: string;
  scopes?: string[];
  offlineAccess?: boolean;
  hostedDomain?: string;
  forceCodeForRefreshToken?: boolean;
  accountName?: string;
  googleServicePlistPath?: string;
  openIdRealm?: string;
  profileImageSize?: number;
}

export interface SignInParams {
  scopes?: string[];
  nonce?: string;
}

export interface AddScopesParams {
  scopes: string[];
}

export interface HasPlayServicesParams {
  showPlayServicesUpdateDialog?: boolean;
}

export interface User {
  id: string;
  name: string;
  email: string;
  photo?: string;
  familyName?: string;
  givenName?: string;
}

export interface SignInResponse {
  user: User;
  scopes: string[];
  serverAuthCode?: string;
  idToken?: string;
}

export interface SignInSilentlyResponse {
  user: User;
  scopes: string[];
  serverAuthCode?: string;
  idToken?: string;
}

export interface GetTokensResponse {
  user: User;
  scopes: string[];
  serverAuthCode?: string;
  idToken?: string;
  accessToken: string;
}

/**
 * Error codes that are consistent across iOS and Android platforms
 */
export type GoogleSignInErrorCode = 
  | 'sign_in_cancelled'      // User cancelled the sign in
  | 'sign_in_required'       // Sign in required
  | 'sign_in_error'          // Generic sign in error
  | 'not_configured'         // Google Sign In is not configured
  | 'no_activity'            // No current activity available
  | 'no_credential'          // No credential available
  | 'parsing_error'          // Failed to parse Google ID token (Android only)
  | 'play_services_not_available' // Play services not available (Android only)
  | 'network_error'          // Network error
  | 'authorization_error'    // Authorization error
  | 'authorization_cancelled' // User cancelled authorization
  | 'unknown_error';         // Unknown error occurred

export interface Spec extends TurboModule {
  // Configuration
  configure(config: ConfigureParams): void;
  
  // Sign In
  signIn(options: SignInParams | null): Promise<SignInResponse>;
  signInSilently(): Promise<SignInSilentlyResponse>;
  addScopes(scopes: string[]): Promise<SignInResponse | null>;
  
  // Sign Out
  signOut(): Promise<void>;
  revokeAccess(): Promise<void>;
  
  // User State
  isSignedIn(): Promise<boolean>;
  getCurrentUser(): Promise<User | null>;
  
  // Utilities
  clearCachedAccessToken(accessToken: string): Promise<void>;
  getTokens(): Promise<GetTokensResponse>;
  hasPlayServices(options: HasPlayServicesParams | null): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RnGoogleSignin');
