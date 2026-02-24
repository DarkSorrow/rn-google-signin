#import "RnGoogleSignin.h"
#import <React/RCTBridgeModule.h>
#import <React/RCTLog.h>
#import <GoogleSignIn/GoogleSignIn.h>
#import <objc/runtime.h>

@implementation RnGoogleSignin

RCT_EXPORT_MODULE()

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeRnGoogleSigninSpecJSI>(params);
}

- (void)configure:(JS::NativeRnGoogleSignin::ConfigureParams &)config {
    @try {
        NSString *clientId = nil;
        
        // Safely extract client ID with proper nil checks
        if (config.iosClientId() && config.iosClientId().length > 0) {
            clientId = config.iosClientId();
        } else if (config.webClientId() && config.webClientId().length > 0) {
            clientId = config.webClientId();
        } else {
            // Fallback to GoogleService-Info.plist
            NSString *path = [[NSBundle mainBundle] pathForResource:@"GoogleService-Info" ofType:@"plist"];
            if (path) {
                NSDictionary *plist = [NSDictionary dictionaryWithContentsOfFile:path];
                if (plist && plist[@"CLIENT_ID"]) {
                    clientId = plist[@"CLIENT_ID"];
                }
            }
        }
        
        // Validate client ID
        if (!clientId || clientId.length == 0) {
            RCTLogError(@"[RnGoogleSignin] configure: No valid client ID found");
            return;
        }
        
        // Configure Google Sign-In
        GIDConfiguration *gidConfig = [[GIDConfiguration alloc] initWithClientID:clientId];
        if (!gidConfig) {
            RCTLogError(@"[RnGoogleSignin] configure: Failed to create GIDConfiguration");
            return;
        }
        
        [GIDSignIn sharedInstance].configuration = gidConfig;
        
        // Handle scopes safely
        if (config.scopes().has_value()) {
            auto scopes = config.scopes().value();
            if (!scopes.empty()) {
                NSMutableArray *scopesArray = [NSMutableArray array];
                for (NSString *scope : scopes) {
                    if (scope && scope.length > 0) {
                        [scopesArray addObject:scope];
                    }
                }
                if (scopesArray.count > 0) {
                    objc_setAssociatedObject(self, "defaultScopes", scopesArray, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
                }
            }
        }
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] configure crashed: %@", exception);
    }
}

- (void)signIn:(JS::NativeRnGoogleSignin::SignInParams &)options
       resolve:(RCTPromiseResolveBlock)resolve
        reject:(RCTPromiseRejectBlock)reject {
    @try {
        // Validate presenting view controller
        UIViewController *presentingViewController = [self getCurrentViewController];
        if (!presentingViewController) {
            reject(@"no_activity", @"No current activity available", nil);
            return;
        }
        
        // Safely handle scopes and nonce from TurboModule struct
        NSArray *scopes = nil;
        NSString *nonce = nil;
        
        // Simple, performant validation and access
        @try {
            // Access nonce from TurboModule struct
            if (options.nonce() && options.nonce().length > 0) {
                nonce = options.nonce();
            }
            
            // Access scopes from TurboModule struct
            // Handle scopes safely
            if (options.scopes().has_value()) {
                auto scopesVector = options.scopes().value();
                if (!scopesVector.empty()) {
                    NSMutableArray *scopesArray = [NSMutableArray array];
                    for (NSString *scope : scopesVector) {
                        if (scope && scope.length > 0) {
                            [scopesArray addObject:scope];
                        }
                    }
                    if (scopesArray.count > 0) {
                        scopes = scopesArray;
                    }
                }
            }
        } @catch (NSException *exception) {
            RCTLogError(@"[RnGoogleSignin] signIn options access error: %@", exception);
            // Continue with default values
        }
        
        // Generate nonce if not provided
        if (!nonce || nonce.length == 0) {
            nonce = [self generateNonce];
        }
        
        // Fallback to default scopes if none provided
        if (!scopes || scopes.count == 0) {
            scopes = objc_getAssociatedObject(self, "defaultScopes");
            if (!scopes) {
                scopes = @[@"profile", @"openid"];
            }
        }
        
        // Validate GIDSignIn instance
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        // Perform sign in with comprehensive error handling
        [signIn signInWithPresentingViewController:presentingViewController
                                            hint:nonce
                                  additionalScopes:scopes
                                  nonce:nonce
                                        completion:^(GIDSignInResult * _Nullable result, NSError * _Nullable error) {
            if (error) {
                NSString *errorCode = @"sign_in_error";
                NSString *errorMessage = error.localizedDescription ?: @"Unknown sign in error";
                
                if (error.code == kGIDSignInErrorCodeCanceled) {
                    errorCode = @"sign_in_cancelled";
                    errorMessage = @"User cancelled the sign in";
                } else if (error.code == kGIDSignInErrorCodeHasNoAuthInKeychain) {
                    errorCode = @"sign_in_required";
                    errorMessage = @"No previous sign in found";
                }
                
                reject(errorCode, errorMessage, error);
                return;
            }
            
            if (!result) {
                reject(@"sign_in_error", @"Sign in failed: No user data received", nil);
                return;
            }
            
            if (!result.user) {
                reject(@"sign_in_error", @"Sign in failed: No user data in result", nil);
                return;
            }
            
            NSDictionary *userData = [self convertSignInResponse:result.user];
            if (!userData) {
                reject(@"sign_in_error", @"Failed to convert user data", nil);
                return;
            }
            
            resolve(userData);
        }];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] signIn crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)signInSilently:(RCTPromiseResolveBlock)resolve
                reject:(RCTPromiseRejectBlock)reject {
    @try {
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        [signIn restorePreviousSignInWithCompletion:^(GIDGoogleUser * _Nullable user, NSError * _Nullable error) {
            if (error) {
                NSString *errorCode = @"sign_in_error";
                NSString *errorMessage = error.localizedDescription ?: @"Unknown sign in error";
                
                if (error.code == kGIDSignInErrorCodeHasNoAuthInKeychain) {
                    errorCode = @"sign_in_required";
                    errorMessage = @"No previous sign in found";
                }
                
                reject(errorCode, errorMessage, error);
                return;
            }
            
            if (!user) {
                reject(@"sign_in_required", @"No previous sign in found", nil);
                return;
            }
            
            NSDictionary *userData = [self convertSignInSilentlyResponse:user];
            if (!userData) {
                reject(@"sign_in_error", @"Failed to convert user data", nil);
                return;
            }
            
            resolve(userData);
        }];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] signInSilently crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)addScopes:(NSArray *)scopes
          resolve:(RCTPromiseResolveBlock)resolve
           reject:(RCTPromiseRejectBlock)reject {
    @try {
        // Validate scopes array
        if (!scopes || scopes.count == 0) {
            reject(@"invalid_scopes", @"No scopes provided", nil);
            return;
        }
        
        // Validate each scope
        NSMutableArray *validScopes = [NSMutableArray array];
        for (id scope in scopes) {
            if ([scope isKindOfClass:[NSString class]] && ((NSString *)scope).length > 0) {
                [validScopes addObject:scope];
            }
        }
        
        if (validScopes.count == 0) {
            reject(@"invalid_scopes", @"No valid scopes provided", nil);
            return;
        }
        
        GIDGoogleUser *currentUser = [GIDSignIn sharedInstance].currentUser;
        if (!currentUser) {
            resolve([NSNull null]);
            return;
        }
        
        UIViewController *presentingViewController = [self getCurrentViewController];
        if (!presentingViewController) {
            reject(@"no_activity", @"No presenting view controller available", nil);
            return;
        }
        
        [currentUser addScopes:validScopes
    presentingViewController:presentingViewController
                 completion:^(GIDSignInResult * _Nullable result, NSError * _Nullable error) {
            if (error) {
                NSString *errorCode = @"sign_in_error";
                NSString *errorMessage = error.localizedDescription ?: @"Unknown error adding scopes";
                
                if (error.code == kGIDSignInErrorCodeCanceled) {
                    errorCode = @"sign_in_cancelled";
                    errorMessage = @"User cancelled adding scopes";
                }
                
                reject(errorCode, errorMessage, error);
                return;
            }
            
            if (!result) {
                resolve([NSNull null]);
                return;
            }
            
            NSDictionary *userData = [self convertSignInResponse:result.user];
            if (!userData) {
                reject(@"sign_in_error", @"Failed to convert user data", nil);
                return;
            }
            
            resolve(userData);
        }];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] addScopes crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)signOut:(RCTPromiseResolveBlock)resolve
        reject:(RCTPromiseRejectBlock)reject {
    @try {
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        [signIn signOut];
        resolve(nil);
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] signOut crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)revokeAccess:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject {
    @try {
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        [signIn disconnectWithCompletion:^(NSError * _Nullable error) {
            if (error) {
                reject(@"sign_in_error", error.localizedDescription ?: @"Failed to revoke access", error);
                return;
            }
            resolve(nil);
        }];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] revokeAccess crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)isSignedIn:(RCTPromiseResolveBlock)resolve
           reject:(RCTPromiseRejectBlock)reject {
    @try {
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        BOOL signedIn = signIn.currentUser != nil;
        resolve(@(signedIn));
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] isSignedIn crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)getCurrentUser:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject {
    @try {
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        GIDGoogleUser *currentUser = signIn.currentUser;
        if (!currentUser) {
            resolve([NSNull null]);
            return;
        }
        
        NSDictionary *userData = [self convertUser:currentUser];
        if (!userData) {
            reject(@"sign_in_error", @"Failed to convert user data", nil);
            return;
        }
        
        resolve(userData);
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] getCurrentUser crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)clearCachedAccessToken:(NSString *)accessToken
                       resolve:(RCTPromiseResolveBlock)resolve
                        reject:(RCTPromiseRejectBlock)reject {
    @try {
        // Not supported on iOS, tokens are managed by SDK
        resolve(nil);
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] clearCachedAccessToken crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)getTokens:(RCTPromiseResolveBlock)resolve
          reject:(RCTPromiseRejectBlock)reject {
    @try {
        GIDSignIn *signIn = [GIDSignIn sharedInstance];
        if (!signIn) {
            reject(@"sign_in_error", @"Google Sign-In not available", nil);
            return;
        }
        
        GIDGoogleUser *currentUser = signIn.currentUser;
        if (!currentUser) {
            reject(@"sign_in_required", @"No user is currently signed in", nil);
            return;
        }
        
        [currentUser refreshTokensIfNeededWithCompletion:^(GIDGoogleUser * _Nullable user, NSError * _Nullable error) {
            if (error) {
                reject(@"sign_in_error", error.localizedDescription ?: @"Failed to refresh tokens", error);
                return;
            }
            
            if (!user) {
                reject(@"sign_in_error", @"No user data after token refresh", nil);
                return;
            }
            
            if (!user.accessToken || !user.accessToken.tokenString) {
                reject(@"sign_in_error", @"No access token available", nil);
                return;
            }
            
            NSMutableDictionary *tokens = [NSMutableDictionary dictionary];
            tokens[@"accessToken"] = user.accessToken.tokenString;
            tokens[@"scopes"] = user.grantedScopes ?: @[];
            tokens[@"idToken"] = user.idToken.tokenString ?: [NSNull null];
            tokens[@"serverAuthCode"] = [NSNull null]; // Not available in latest SDK
            
            NSDictionary *userData = [self convertUser:user];
            if (userData && userData[@"user"]) {
                tokens[@"user"] = userData[@"user"];
            } else {
                tokens[@"user"] = [NSNull null];
            }
            
            resolve(tokens);
        }];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] getTokens crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

- (void)hasPlayServices:(JS::NativeRnGoogleSignin::HasPlayServicesParams &)params
                resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject {
    @try {
        // Always true on iOS - options can be null, so treat as optional
        resolve(@YES);
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] hasPlayServices crashed: %@", exception);
        reject(@"native_crash", exception.reason ?: @"Unknown native error", nil);
    }
}

// MARK: - Helpers
- (UIViewController *)getCurrentViewController {
    @try {
        // Ensure we're on the main thread for UI operations
        if (![NSThread isMainThread]) {
            __block UIViewController *result = nil;
            dispatch_sync(dispatch_get_main_queue(), ^{
                result = [self getCurrentViewController];
            });
            return result;
        }
        
        UIApplication *application = [UIApplication sharedApplication];
        if (!application) {
            RCTLogError(@"[RnGoogleSignin] getCurrentViewController: No UIApplication available");
            return nil;
        }
        
        UIWindow *window = nil;
        
        // Use modern iOS 13+ API to get the key window
        NSSet<UIScene *> *connectedScenes = application.connectedScenes;
        for (UIScene *scene in connectedScenes) {
            if ([scene isKindOfClass:[UIWindowScene class]]) {
                UIWindowScene *windowScene = (UIWindowScene *)scene;
                if (windowScene.activationState == UISceneActivationStateForegroundActive) {
                    for (UIWindow *w in windowScene.windows) {
                        if (w.isKeyWindow) {
                            window = w;
                            break;
                        }
                    }
                    if (window) break;
                }
            }
        }
        
        if (!window) {
            RCTLogError(@"[RnGoogleSignin] getCurrentViewController: No key window found");
            return nil;
        }
        
        UIViewController *rootViewController = window.rootViewController;
        if (!rootViewController) {
            RCTLogError(@"[RnGoogleSignin] getCurrentViewController: No root view controller");
            return nil;
        }
        
        return [self getTopViewController:rootViewController];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] getCurrentViewController crashed: %@", exception);
        return nil;
    }
}

- (NSString *)generateNonce {
    @try {
        NSMutableData *data = [NSMutableData dataWithLength:32];
        int result = SecRandomCopyBytes(kSecRandomDefault, 32, data.mutableBytes);
        if (result == 0) {
            return [data base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength | NSDataBase64EncodingEndLineWithLineFeed];
        } else {
            // Fallback to arc4random if SecRandomCopyBytes fails
            NSMutableData *fallbackData = [NSMutableData dataWithLength:32];
            for (int i = 0; i < 32; i++) {
                uint8_t byte = (uint8_t)arc4random_uniform(256);
                [fallbackData replaceBytesInRange:NSMakeRange(i, 1) withBytes:&byte];
            }
            return [fallbackData base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength | NSDataBase64EncodingEndLineWithLineFeed];
        }
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] generateNonce crashed: %@", exception);
        // Return a simple fallback nonce
        return @"fallback-nonce";
    }
}

- (UIViewController *)getTopViewController:(UIViewController *)base {
    @try {
        if (!base) {
            return nil;
        }
        
        if ([base isKindOfClass:[UINavigationController class]]) {
            UINavigationController *nav = (UINavigationController *)base;
            UIViewController *visibleVC = nav.visibleViewController;
            return [self getTopViewController:visibleVC ?: nav];
        } else if ([base isKindOfClass:[UITabBarController class]]) {
            UITabBarController *tab = (UITabBarController *)base;
            if (tab.selectedViewController) {
                return [self getTopViewController:tab.selectedViewController];
            }
        } else if (base.presentedViewController) {
            return [self getTopViewController:base.presentedViewController];
        }
        return base;
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] getTopViewController crashed: %@", exception);
        return base;
    }
}

- (NSDictionary *)convertUser:(GIDGoogleUser *)user {
    @try {
        if (!user) {
            return nil;
        }
        
        NSMutableDictionary *userInfo = [NSMutableDictionary dictionary];
        userInfo[@"id"] = user.userID ?: @"";
        userInfo[@"name"] = user.profile.name ?: [NSNull null];
        userInfo[@"email"] = user.profile.email ?: [NSNull null];
        
        NSURL *photoURL = [user.profile imageURLWithDimension:120];
        userInfo[@"photo"] = photoURL.absoluteString ?: [NSNull null];
        userInfo[@"familyName"] = user.profile.familyName ?: [NSNull null];
        userInfo[@"givenName"] = user.profile.givenName ?: [NSNull null];
        
        return @{@"user": userInfo};
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] convertUser crashed: %@", exception);
        return nil;
    }
}

- (NSDictionary *)convertSignInResponse:(GIDGoogleUser *)user {
    @try {
        NSDictionary *userData = [self convertUser:user];
        if (!userData) {
            return nil;
        }
        
        NSMutableDictionary *dict = [userData mutableCopy];
        dict[@"scopes"] = user.grantedScopes ?: @[];
        dict[@"serverAuthCode"] = [NSNull null]; // Not available in latest SDK
        dict[@"idToken"] = user.idToken.tokenString ?: [NSNull null];
        
        return dict;
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] convertSignInResponse crashed: %@", exception);
        return nil;
    }
}

- (NSDictionary *)convertSignInSilentlyResponse:(GIDGoogleUser *)user {
    @try {
        return [self convertSignInResponse:user];
    }
    @catch (NSException *exception) {
        RCTLogError(@"[RnGoogleSignin] convertSignInSilentlyResponse crashed: %@", exception);
        return nil;
    }
}

@end 