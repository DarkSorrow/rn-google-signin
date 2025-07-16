import RnGoogleSignin from '../index';

// Mock the native module
jest.mock('react-native', () => ({
  TurboModuleRegistry: {
    getEnforcing: jest.fn(() => ({
      configure: jest.fn(),
      signIn: jest.fn(),
      signInSilently: jest.fn(),
      addScopes: jest.fn(),
      signOut: jest.fn(),
      revokeAccess: jest.fn(),
      isSignedIn: jest.fn(),
      getCurrentUser: jest.fn(),
      clearCachedAccessToken: jest.fn(),
      getTokens: jest.fn(),
      hasPlayServices: jest.fn(),
    })),
  },
  NativeModules: {
    RnGoogleSignin: {
      configure: jest.fn(),
      signIn: jest.fn(),
      signInSilently: jest.fn(),
      addScopes: jest.fn(),
      signOut: jest.fn(),
      revokeAccess: jest.fn(),
      isSignedIn: jest.fn(),
      getCurrentUser: jest.fn(),
      clearCachedAccessToken: jest.fn(),
      getTokens: jest.fn(),
      hasPlayServices: jest.fn(),
    },
  },
  DeviceEventEmitter: {
    addListener: jest.fn(),
    removeListener: jest.fn(),
  },
}));

describe('RnGoogleSignin', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should export the module', () => {
    expect(RnGoogleSignin).toBeDefined();
    expect(typeof RnGoogleSignin.configure).toBe('function');
    expect(typeof RnGoogleSignin.signIn).toBe('function');
    expect(typeof RnGoogleSignin.signInSilently).toBe('function');
    expect(typeof RnGoogleSignin.addScopes).toBe('function');
    expect(typeof RnGoogleSignin.signOut).toBe('function');
    expect(typeof RnGoogleSignin.revokeAccess).toBe('function');
    expect(typeof RnGoogleSignin.isSignedIn).toBe('function');
    expect(typeof RnGoogleSignin.getCurrentUser).toBe('function');
    expect(typeof RnGoogleSignin.clearCachedAccessToken).toBe('function');
    expect(typeof RnGoogleSignin.getTokens).toBe('function');
    expect(typeof RnGoogleSignin.hasPlayServices).toBe('function');
  });

  it('should have all required methods', () => {
    const methods = [
      'configure',
      'signIn',
      'signInSilently',
      'addScopes',
      'signOut',
      'revokeAccess',
      'isSignedIn',
      'getCurrentUser',
      'clearCachedAccessToken',
      'getTokens',
      'hasPlayServices'
    ];

    methods.forEach(method => {
      expect((RnGoogleSignin as any)[method]).toBeDefined();
      expect(typeof (RnGoogleSignin as any)[method]).toBe('function');
    });
  });
});
