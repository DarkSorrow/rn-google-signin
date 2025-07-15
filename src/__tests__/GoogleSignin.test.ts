import { GoogleSignInError, statusCodes } from '../types';

describe('GoogleSignInError', () => {
  it('should create an error with message and code', () => {
    const error = new GoogleSignInError('Test error', 'TEST_CODE');
    
    expect(error.message).toBe('Test error');
    expect(error.code).toBe('TEST_CODE');
    expect(error.name).toBe('GoogleSignInError');
    expect(error instanceof Error).toBe(true);
  });
});

describe('statusCodes', () => {
  it('should have all required status codes', () => {
    expect(statusCodes.SIGN_IN_CANCELLED).toBe('SIGN_IN_CANCELLED');
    expect(statusCodes.IN_PROGRESS).toBe('IN_PROGRESS');
    expect(statusCodes.PLAY_SERVICES_NOT_AVAILABLE).toBe('PLAY_SERVICES_NOT_AVAILABLE');
    expect(statusCodes.SIGN_IN_REQUIRED).toBe('SIGN_IN_REQUIRED');
  });
});

describe('GoogleSignIn Module', () => {
  // Mock the native module since we can't test the actual implementation in Jest
  const mockNativeModule = {
    configure: jest.fn(),
    hasPlayServices: jest.fn(),
    signIn: jest.fn(),
    signInSilently: jest.fn(),
    addScopes: jest.fn(),
    signOut: jest.fn(),
    revokeAccess: jest.fn(),
    isSignedIn: jest.fn(),
    getCurrentUser: jest.fn(),
    clearCachedAccessToken: jest.fn(),
    getTokens: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should be importable', () => {
    expect(() => require('../index')).not.toThrow();
  });
}); 