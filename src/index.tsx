import RnGoogleSignin from './NativeRnGoogleSignin';

// Re-export types for users
export type {
  ConfigureParams,
  SignInParams,
  AddScopesParams,
  HasPlayServicesParams,
  User,
  SignInResponse,
  SignInSilentlyResponse,
  GetTokensResponse,
} from './NativeRnGoogleSignin';

// Export the module as default
export default RnGoogleSignin;
