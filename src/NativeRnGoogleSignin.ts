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

export interface Spec extends TurboModule {
  // Configuration
  configure(config: ConfigureParams): void;
  
  // Sign In
  signIn(options?: SignInParams): Promise<SignInResponse>;
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
  hasPlayServices(options?: HasPlayServicesParams): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RnGoogleSignin');
