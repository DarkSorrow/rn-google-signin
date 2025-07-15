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

export interface GoogleSignInStatusCodes {
  SIGN_IN_CANCELLED: string;
  IN_PROGRESS: string;
  PLAY_SERVICES_NOT_AVAILABLE: string;
  SIGN_IN_REQUIRED: string;
}

export class GoogleSignInError extends Error {
  code: string;

  constructor(message: string, code: string) {
    super(message);
    this.code = code;
    this.name = 'GoogleSignInError';
  }
}

export const statusCodes: GoogleSignInStatusCodes = {
  SIGN_IN_CANCELLED: 'SIGN_IN_CANCELLED',
  IN_PROGRESS: 'IN_PROGRESS', 
  PLAY_SERVICES_NOT_AVAILABLE: 'PLAY_SERVICES_NOT_AVAILABLE',
  SIGN_IN_REQUIRED: 'SIGN_IN_REQUIRED',
} as const; 