import NativeGoogleSignin from './spec/NativeGoogleSignin';
import type {
  GoogleSignInUser,
  GoogleSignInConfig,
  GoogleSignInTokens,
} from './types';
import { GoogleSignInError, statusCodes } from './types';

class GoogleSignIn {
  private _isConfigured = false;

  /**
   * Configure the Google Sign-In instance with your app's credentials.
   * Must be called before any other methods.
   */
  async configure(config: GoogleSignInConfig): Promise<void> {
    try {
      await NativeGoogleSignin.configure(config);
      this._isConfigured = true;
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Check if Google Play Services are available (Android only).
   * Returns true on iOS.
   */
  async hasPlayServices(): Promise<boolean> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.hasPlayServices();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Prompt the user to sign in with their Google account.
   */
  async signIn(): Promise<GoogleSignInUser> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.signIn();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Sign in silently if the user is already authenticated.
   */
  async signInSilently(): Promise<GoogleSignInUser> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.signInSilently();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Add additional scopes to the current user.
   */
  async addScopes(scopes: string[]): Promise<GoogleSignInUser> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.addScopes(scopes);
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Sign out the current user.
   */
  async signOut(): Promise<void> {
    this._assertConfigured();
    try {
      await NativeGoogleSignin.signOut();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Revoke access and sign out the user.
   */
  async revokeAccess(): Promise<void> {
    this._assertConfigured();
    try {
      await NativeGoogleSignin.revokeAccess();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Check if a user is currently signed in.
   */
  async isSignedIn(): Promise<boolean> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.isSignedIn();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Get the current signed-in user, or null if none.
   */
  async getCurrentUser(): Promise<GoogleSignInUser | null> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.getCurrentUser();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Clear the cached access token.
   */
  async clearCachedAccessToken(accessToken: string): Promise<void> {
    this._assertConfigured();
    try {
      await NativeGoogleSignin.clearCachedAccessToken(accessToken);
    } catch (error) {
      throw this._handleError(error);
    }
  }

  /**
   * Get the current access token and ID token.
   */
  async getTokens(): Promise<GoogleSignInTokens> {
    this._assertConfigured();
    try {
      return await NativeGoogleSignin.getTokens();
    } catch (error) {
      throw this._handleError(error);
    }
  }

  private _assertConfigured(): void {
    if (!this._isConfigured) {
      throw new GoogleSignInError(
        'GoogleSignIn must be configured before calling this method',
        statusCodes.SIGN_IN_REQUIRED
      );
    }
  }

  private _handleError(error: any): Error {
    if (error instanceof GoogleSignInError) {
      return error;
    }

    // Convert native errors to GoogleSignInError
    const message = error?.message || 'Unknown error occurred';
    const code = error?.code || 'UNKNOWN_ERROR';
    
    return new GoogleSignInError(message, code);
  }
}

// Export a singleton instance
export default new GoogleSignIn(); 