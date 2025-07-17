#import "RnGoogleSignin.h"
#import <GoogleSignIn/GoogleSignIn.h>
#import <React/RCTLog.h>
#import <objc/runtime.h>

@implementation RnGoogleSignin

// MARK: - TurboModule Implementation
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeRnGoogleSigninSpecJSI>(params);
}

// MARK: - Configuration
- (void)configure:(NSDictionary *)config
{
    NSString *clientId = nil;
    
    if (config[@"iosClientId"]) {
        clientId = config[@"iosClientId"];
    } else if (config[@"webClientId"]) {
        clientId = config[@"webClientId"];
    } else {
        NSString *path = [[NSBundle mainBundle] pathForResource:@"GoogleService-Info" ofType:@"plist"];
        if (path) {
            NSDictionary *plist = [NSDictionary dictionaryWithContentsOfFile:path];
            clientId = plist[@"CLIENT_ID"];
        }
    }
    
    if (clientId && clientId.length > 0) {
        GIDConfiguration *gidConfig = [[GIDConfiguration alloc] initWithClientID:clientId];
        [GIDSignIn sharedInstance].configuration = gidConfig;
        
        // Store scopes for later use
        NSArray *scopes = config[@"scopes"];
        if (scopes) {
            objc_setAssociatedObject(self, "defaultScopes", scopes, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        }
    }
}

// MARK: - Sign In
- (void)signIn:(NSDictionary * _Nullable)options
       resolve:(RCTPromiseResolveBlock)resolve
        reject:(RCTPromiseRejectBlock)reject
{
    UIViewController *presentingViewController = [self getCurrentViewController];
    if (!presentingViewController) {
        reject(@"no_activity", @"No current activity available", nil);
        return;
    }
    
    NSArray *scopes = options[@"scopes"];
    if (!scopes) {
        scopes = objc_getAssociatedObject(self, "defaultScopes");
        if (!scopes) {
            scopes = @[@"profile", @"email"];
        }
    }
    
    [[GIDSignIn sharedInstance] signInWithPresentingViewController:presentingViewController
                                                             hint:nil
                                                   additionalScopes:scopes
                                                         completion:^(GIDSignInResult * _Nullable result, NSError * _Nullable error) {
        if (error) {
            if (error.code == kGIDSignInErrorCodeCanceled) {
                reject(@"sign_in_cancelled", @"User cancelled the sign in", error);
            } else {
                reject(@"sign_in_error", error.localizedDescription, error);
            }
            return;
        }
        
        if (!result) {
            reject(@"sign_in_error", @"Sign in failed: No user data received", nil);
            return;
        }
        
        resolve([self convertSignInResponse:result.user]);
    }];
}

- (void)signInSilentlyWithResolve:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject
{
    [[GIDSignIn sharedInstance] restorePreviousSignInWithCompletion:^(GIDGoogleUser * _Nullable user, NSError * _Nullable error) {
        if (error) {
            if (error.code == kGIDSignInErrorCodeHasNoAuthInKeychain) {
                reject(@"sign_in_required", @"No previous sign in found", error);
            } else {
                reject(@"sign_in_error", error.localizedDescription, error);
            }
            return;
        }
        
        if (!user) {
            reject(@"sign_in_required", @"No previous sign in found", nil);
            return;
        }
        
        resolve([self convertSignInSilentlyResponse:user]);
    }];
}

- (void)addScopes:(NSArray<NSString *> *)scopes
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject
{
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
    
    [currentUser addScopes:scopes
            presentingViewController:presentingViewController
                         completion:^(GIDSignInResult * _Nullable result, NSError * _Nullable error) {
        if (error) {
            if (error.code == kGIDSignInErrorCodeCanceled) {
                reject(@"sign_in_cancelled", @"User cancelled adding scopes", error);
            } else {
                reject(@"sign_in_error", error.localizedDescription, error);
            }
            return;
        }
        
        if (!result) {
            resolve([NSNull null]);
            return;
        }
        
        resolve([self convertSignInResponse:result.user]);
    }];
}

// MARK: - Sign Out
- (void)signOutWithResolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
    [[GIDSignIn sharedInstance] signOut];
    resolve(nil);
}

- (void)revokeAccessWithResolve:(RCTPromiseResolveBlock)resolve
                         reject:(RCTPromiseRejectBlock)reject
{
    [[GIDSignIn sharedInstance] disconnectWithCompletion:^(NSError * _Nullable error) {
        if (error) {
            reject(@"sign_in_error", error.localizedDescription, error);
            return;
        }
        resolve(nil);
    }];
}

// MARK: - User State
- (void)isSignedInWithResolve:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject
{
    BOOL signedIn = [GIDSignIn sharedInstance].currentUser != nil;
    resolve(@(signedIn));
}

- (void)getCurrentUserWithResolve:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject
{
    GIDGoogleUser *currentUser = [GIDSignIn sharedInstance].currentUser;
    if (!currentUser) {
        resolve([NSNull null]);
        return;
    }
    resolve([self convertUser:currentUser]);
}

// MARK: - Utilities
- (void)clearCachedAccessToken:(NSString *)accessToken
                       resolve:(RCTPromiseResolveBlock)resolve
                        reject:(RCTPromiseRejectBlock)reject
{
    // Not supported on iOS, tokens are managed by SDK
    resolve(nil);
}

- (void)getTokensWithResolve:(RCTPromiseResolveBlock)resolve
                      reject:(RCTPromiseRejectBlock)reject
{
    GIDGoogleUser *currentUser = [GIDSignIn sharedInstance].currentUser;
    if (!currentUser) {
        reject(@"sign_in_required", @"No user is currently signed in", nil);
        return;
    }
    
    [currentUser refreshTokensIfNeededWithCompletion:^(GIDGoogleUser * _Nullable user, NSError * _Nullable error) {
        if (error) {
            reject(@"sign_in_error", error.localizedDescription, error);
            return;
        }
        
        if (!user || !user.accessToken.tokenString) {
            reject(@"sign_in_error", @"No access token available", nil);
            return;
        }
        
        NSMutableDictionary *tokens = [NSMutableDictionary dictionary];
        tokens[@"accessToken"] = user.accessToken.tokenString;
        tokens[@"scopes"] = user.grantedScopes ?: @[];
        tokens[@"idToken"] = user.idToken.tokenString ?: [NSNull null];
        tokens[@"serverAuthCode"] = [NSNull null]; // Not available in latest SDK
        tokens[@"user"] = [self convertUser:user][@"user"] ?: [NSNull null];
        
        resolve(tokens);
    }];
}

- (void)hasPlayServices:(NSDictionary * _Nullable)options
                resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject
{
    // Always true on iOS
    resolve(@YES);
}

// MARK: - Helpers
- (UIViewController *)getCurrentViewController {
    UIWindow *window = [UIApplication sharedApplication].keyWindow;
    if (!window) {
        NSArray *windows = [UIApplication sharedApplication].windows;
        for (UIWindow *w in windows) {
            if (w.isKeyWindow) {
                window = w;
                break;
            }
        }
    }
    
    UIViewController *rootViewController = window.rootViewController;
    return [self getTopViewController:rootViewController];
}

- (UIViewController *)getTopViewController:(UIViewController *)base {
    if ([base isKindOfClass:[UINavigationController class]]) {
        UINavigationController *nav = (UINavigationController *)base;
        return [self getTopViewController:nav.visibleViewController ?: nav];
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

- (NSDictionary *)convertUser:(GIDGoogleUser *)user {
    NSMutableDictionary *userInfo = [NSMutableDictionary dictionary];
    userInfo[@"id"] = user.userID ?: @"";
    userInfo[@"name"] = user.profile.name ?: [NSNull null];
    userInfo[@"email"] = user.profile.email ?: [NSNull null];
    userInfo[@"photo"] = [user.profile imageURLWithDimension:120].absoluteString ?: [NSNull null];
    userInfo[@"familyName"] = user.profile.familyName ?: [NSNull null];
    userInfo[@"givenName"] = user.profile.givenName ?: [NSNull null];
    
    return @{@"user": userInfo};
}

- (NSDictionary *)convertSignInResponse:(GIDGoogleUser *)user {
    NSMutableDictionary *dict = [[self convertUser:user] mutableCopy];
    dict[@"scopes"] = user.grantedScopes ?: @[];
    dict[@"serverAuthCode"] = [NSNull null]; // Not available in latest SDK
    dict[@"idToken"] = user.idToken.tokenString ?: [NSNull null];
    return dict;
}

- (NSDictionary *)convertSignInSilentlyResponse:(GIDGoogleUser *)user {
    return [self convertSignInResponse:user];
}

@end 