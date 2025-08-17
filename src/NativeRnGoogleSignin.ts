import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

/**
 * Configuration parameters for Google Sign-In
 * Note: Not all parameters are supported on all platforms
 */
export interface ConfigureParams {
  /**
   * The web client ID from Google Console
   * Required for Android and iOS
   */
  webClientId?: string;

  /**
   * Android-specific client ID (optional)
   * If not provided, webClientId will be used
   */
  androidClientId?: string;

  /**
   * iOS-specific client ID (optional)
   */
  iosClientId?: string;

  /**
   * Additional OAuth scopes to request
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  scopes?: string[];

  /**
   * Request offline access to get server auth code
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  offlineAccess?: boolean;

  /**
   * Restrict sign-in to accounts from a specific domain
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  hostedDomain?: string;

  /**
   * Force code for refresh token
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  forceCodeForRefreshToken?: boolean;

  /**
   * Pre-fill the account name
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  accountName?: string;

  /**
   * Path to GoogleService-Info.plist
   * @platform iOS
   */
  googleServicePlistPath?: string;

  /**
   * OpenID realm
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  openIdRealm?: string;

  /**
   * Profile image size to request
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  profileImageSize?: number;
}

export interface SignInParams {
  /**
   * Additional scopes to request during sign-in
   * Note: Not supported on Android with CredentialManager
   * @platform iOS
   */
  scopes?: string[];

  /**
   * Nonce for security (will be auto-generated if not provided)
   */
  nonce?: string;
}

export interface AddScopesParams {
  scopes: string[];
}

export interface HasPlayServicesParams {
  /**
   * Show dialog to update Play Services if needed
   * @platform Android
   */
  showPlayServicesUpdateDialog?: boolean;
}

export interface User {
  id: string;
  name: string | null;
  email: string;
  photo?: string | null;
  familyName?: string | null;
  givenName?: string | null;
}

export interface SignInResponse {
  user: User;
  scopes: string[];
  serverAuthCode?: string | null;
  idToken?: string | null;
}

export interface SignInSilentlyResponse {
  user: User;
  scopes: string[];
  serverAuthCode?: string | null;
  idToken?: string | null;
}

/**
 * Response from getTokens()
 * Note: On Android with CredentialManager, accessToken is the same as idToken
 */
export interface GetTokensResponse {
  idToken?: string | null;
  accessToken?: string | null;
}

/**
 * Error codes that can be thrown by the module
 * Some errors are platform-specific
 */
export type GoogleSignInErrorCode =
  // Common errors
  | 'sign_in_cancelled'           // User cancelled the sign in
  | 'sign_in_required'            // Sign in required (no user signed in)
  | 'sign_in_error'               // Generic sign in error
  | 'not_configured'              // Google Sign In is not configured
  | 'no_credential'               // No credential available
  | 'network_error'               // Network error during operation
  | 'unknown_error'               // Unknown error occurred

  // Android-specific errors
  | 'no_activity'                 // No current activity available (Android)
  | 'no_valid_activity'           // Activity is invalid or destroyed (Android)
  | 'parsing_error'               // Failed to parse Google ID token (Android)
  | 'play_services_not_available' // Play services not available (Android)
  | 'play_services_error'         // Play services error (Android)
  | 'credential_manager_error'    // Failed to initialize Credential Manager (Android)
  | 'ui_error'                    // Failed to launch selector UI (Android)
  | 'not_supported'               // Operation not supported (e.g., addScopes on Android)
  | 'cancelled'                   // Previous operation was cancelled (Android)

  // iOS-specific errors
  | 'authorization_error'         // Authorization error (iOS)
  | 'authorization_cancelled';    // User cancelled authorization (iOS)

export interface Spec extends TurboModule {
  /**
   * Configure Google Sign-In
   * Must be called before any other methods
   */
  configure(config: ConfigureParams): void;

  /**
   * Sign in with Google
   * Shows the sign-in UI to the user
   */
  signIn(options: SignInParams | null): Promise<SignInResponse>;

  /**
   * Sign in silently (without UI)
   * Only works if user has previously signed in
   */
  signInSilently(): Promise<SignInSilentlyResponse>;

  /**
   * Add additional OAuth scopes
   * Note: Not supported on Android with CredentialManager - will throw 'not_supported'
   * @platform iOS
   */
  addScopes(scopes: string[]): Promise<SignInResponse | null>;

  /**
   * Sign out the current user
   * Clears the cached credentials
   */
  signOut(): Promise<void>;

  /**
   * Revoke access for the current user
   * User will need to re-authorize the app
   */
  revokeAccess(): Promise<void>;

  /**
   * Check if a user is currently signed in
   */
  isSignedIn(): Promise<boolean>;

  /**
   * Get the current signed-in user (if any)
   * Returns null if no user is signed in
   */
  getCurrentUser(): Promise<User | null>;

  /**
   * Clear cached access token
   * Note: No-op on Android with CredentialManager
   */
  clearCachedAccessToken(accessToken: string): Promise<void>;

  /**
   * Get current tokens
   * Note: On Android, accessToken will be the same as idToken
   */
  getTokens(): Promise<GetTokensResponse>;

  /**
   * Check if Google Play Services is available
   * @platform Android
   */
  hasPlayServices(options: HasPlayServicesParams | null): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RnGoogleSignin');
