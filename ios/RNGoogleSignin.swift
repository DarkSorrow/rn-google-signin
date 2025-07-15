import Foundation
import GoogleSignIn
import React

@objc(RNGoogleSignin)
class RNGoogleSignin: NSObject, RCTBridgeModule, RCTTurboModule {
    
    static func moduleName() -> String! {
        return "RNGoogleSignin"
    }
    
    static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    private var isConfigured = false
    private var resolver: RCTPromiseResolveBlock?
    private var rejecter: RCTPromiseRejectBlock?
    
    // MARK: - Configuration
    
    @objc func configure(_ config: NSDictionary, 
                        resolver resolve: @escaping RCTPromiseResolveBlock,
                        rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            do {
                // Get the client ID from config or from GoogleService-Info.plist
                var clientId: String?
                
                if let iosClientId = config["iosClientId"] as? String {
                    clientId = iosClientId
                } else if let webClientId = config["webClientId"] as? String {
                    clientId = webClientId
                } else {
                    // Try to get from GoogleService-Info.plist
                    if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
                       let plist = NSDictionary(contentsOfFile: path),
                       let plistClientId = plist["CLIENT_ID"] as? String {
                        clientId = plistClientId
                    }
                }
                
                guard let finalClientId = clientId else {
                    reject("configuration_error", "No client ID found. Please provide iosClientId, webClientId, or ensure GoogleService-Info.plist is present.", nil)
                    return
                }
                
                // Configure Google Sign In
                guard let config = GIDConfiguration(clientID: finalClientId) else {
                    reject("configuration_error", "Invalid client ID provided", nil)
                    return
                }
                
                // Set additional scopes if provided
                if let scopes = config["scopes"] as? [String] {
                    config.serverClientID = config["webClientId"] as? String
                }
                
                if let hostedDomain = config["hostedDomain"] as? String {
                    config.hostedDomain = hostedDomain
                }
                
                GIDSignIn.sharedInstance.configuration = config
                self.isConfigured = true
                resolve(nil)
                
            } catch {
                reject("configuration_error", "Failed to configure Google Sign In: \(error.localizedDescription)", error)
            }
        }
    }
    
    // MARK: - Sign In Methods
    
    @objc func hasPlayServices(_ resolve: @escaping RCTPromiseResolveBlock,
                              rejecter reject: @escaping RCTPromiseRejectBlock) {
        // Always return true on iOS as Google Play Services is an Android concept
        resolve(true)
    }
    
    @objc func signIn(_ resolve: @escaping RCTPromiseResolveBlock,
                     rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard isConfigured else {
            reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
            return
        }
        
        DispatchQueue.main.async {
            guard let presentingViewController = self.getCurrentViewController() else {
                reject("no_view_controller", "No presenting view controller available", nil)
                return
            }
            
            GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { [weak self] result, error in
                self?.handleSignInResult(result: result, error: error, resolve: resolve, reject: reject)
            }
        }
    }
    
    @objc func signInSilently(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard isConfigured else {
            reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
            return
        }
        
        GIDSignIn.sharedInstance.restorePreviousSignIn { [weak self] user, error in
            if let error = error {
                reject("sign_in_error", "Silent sign in failed: \(error.localizedDescription)", error)
                return
            }
            
            guard let user = user else {
                reject("sign_in_required", "No previous sign in found", nil)
                return
            }
            
            if let userDict = self?.convertUserToDict(user: user) {
                resolve(userDict)
            } else {
                reject("conversion_error", "Failed to convert user data", nil)
            }
        }
    }
    
    @objc func addScopes(_ scopes: [String],
                        resolver resolve: @escaping RCTPromiseResolveBlock,
                        rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard isConfigured else {
            reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
            return
        }
        
        guard let currentUser = GIDSignIn.sharedInstance.currentUser else {
            reject("sign_in_required", "No user is currently signed in", nil)
            return
        }
        
        DispatchQueue.main.async {
            guard let presentingViewController = self.getCurrentViewController() else {
                reject("no_view_controller", "No presenting view controller available", nil)
                return
            }
            
            currentUser.addScopes(scopes, presenting: presentingViewController) { [weak self] user, error in
                if let error = error {
                    reject("add_scopes_error", "Failed to add scopes: \(error.localizedDescription)", error)
                    return
                }
                
                guard let user = user else {
                    reject("add_scopes_error", "Failed to add scopes: No user returned", nil)
                    return
                }
                
                if let userDict = self?.convertUserToDict(user: user) {
                    resolve(userDict)
                } else {
                    reject("conversion_error", "Failed to convert user data", nil)
                }
            }
        }
    }
    
    // MARK: - Sign Out Methods
    
    @objc func signOut(_ resolve: @escaping RCTPromiseResolveBlock,
                      rejecter reject: @escaping RCTPromiseRejectBlock) {
        GIDSignIn.sharedInstance.signOut()
        resolve(nil)
    }
    
    @objc func revokeAccess(_ resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        GIDSignIn.sharedInstance.disconnect { error in
            if let error = error {
                reject("revoke_error", "Failed to revoke access: \(error.localizedDescription)", error)
                return
            }
            resolve(nil)
        }
    }
    
    // MARK: - User State
    
    @objc func isSignedIn(_ resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock) {
        let signedIn = GIDSignIn.sharedInstance.currentUser != nil
        resolve(signedIn)
    }
    
    @objc func getCurrentUser(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard let currentUser = GIDSignIn.sharedInstance.currentUser else {
            resolve(NSNull())
            return
        }
        
        if let userDict = convertUserToDict(user: currentUser) {
            resolve(userDict)
        } else {
            reject("conversion_error", "Failed to convert user data", nil)
        }
    }
    
    // MARK: - Utilities
    
    @objc func clearCachedAccessToken(_ accessToken: String,
                                     resolver resolve: @escaping RCTPromiseResolveBlock,
                                     rejecter reject: @escaping RCTPromiseRejectBlock) {
        // iOS Google Sign In SDK doesn't have a direct equivalent to clearing cached tokens
        // The tokens are automatically managed and refreshed by the SDK
        resolve(nil)
    }
    
    @objc func getTokens(_ resolve: @escaping RCTPromiseResolveBlock,
                        rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        guard let currentUser = GIDSignIn.sharedInstance.currentUser else {
            reject("sign_in_required", "No user is currently signed in", nil)
            return
        }
        
        currentUser.refreshTokensIfNeeded { user, error in
            if let error = error {
                reject("token_error", "Failed to get tokens: \(error.localizedDescription)", error)
                return
            }
            
            guard let user = user,
                  let accessToken = user.accessToken.tokenString else {
                reject("token_error", "Failed to get access token", nil)
                return
            }
            
            let tokens: [String: Any] = [
                "accessToken": accessToken,
                "idToken": user.idToken?.tokenString ?? NSNull()
            ]
            
            resolve(tokens)
        }
    }
    
    // MARK: - Helper Methods
    
    private func handleSignInResult(result: GIDSignInResult?, 
                                   error: Error?, 
                                   resolve: @escaping RCTPromiseResolveBlock,
                                   reject: @escaping RCTPromiseRejectBlock) {
        
        if let error = error {
            let nsError = error as NSError
            let code = nsError.code
            
            switch code {
            case GIDSignInError.canceled.rawValue:
                reject("sign_in_cancelled", "User cancelled the sign in", error)
            case GIDSignInError.EMM.rawValue:
                reject("emm_error", "Enterprise Mobility Management error", error)
            case GIDSignInError.unknown.rawValue:
                reject("unknown_error", "Unknown error occurred", error)
            default:
                reject("sign_in_error", "Sign in failed: \(error.localizedDescription)", error)
            }
            return
        }
        
        guard let result = result,
              let userDict = convertUserToDict(user: result.user) else {
            reject("sign_in_error", "Sign in failed: No user data received", nil)
            return
        }
        
        resolve(userDict)
    }
    
    private func convertUserToDict(user: GIDGoogleUser) -> [String: Any]? {
        guard let profile = user.profile else { return nil }
        
        let userInfo: [String: Any] = [
            "id": user.userID ?? "",
            "name": profile.name ?? NSNull(),
            "email": profile.email,
            "photo": profile.imageURL(withDimension: 120)?.absoluteString ?? NSNull(),
            "familyName": profile.familyName ?? NSNull(),
            "givenName": profile.givenName ?? NSNull()
        ]
        
        let userData: [String: Any] = [
            "user": userInfo,
            "scopes": user.grantedScopes ?? [],
            "serverAuthCode": user.serverAuthCode ?? NSNull(),
            "idToken": user.idToken?.tokenString ?? NSNull()
        ]
        
        return userData
    }
    
    private func getCurrentViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first(where: { $0.isKeyWindow }),
              let rootViewController = window.rootViewController else {
            return nil
        }
        
        return getTopViewController(base: rootViewController)
    }
    
    private func getTopViewController(base: UIViewController) -> UIViewController {
        if let nav = base as? UINavigationController {
            return getTopViewController(base: nav.visibleViewController ?? nav)
        } else if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return getTopViewController(base: selected)
        } else if let presented = base.presentedViewController {
            return getTopViewController(base: presented)
        }
        return base
    }
} 