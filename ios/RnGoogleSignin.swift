import Foundation
import GoogleSignIn
import React
import CryptoKit

@objc(RnGoogleSignin)
class RnGoogleSignin: NSObject, NativeRnGoogleSigninSpec {
    
    private var isConfigured = false
    private var defaultScopes: [String] = []
    private var nonce: String?
    
    // MARK: - Configuration
    
    func configure(config: [String: Any]) {
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
            // Configuration error - but we can't reject since this is not a Promise-based function
            // The error will be handled when trying to use the module
            return
        }
        
        if finalClientId.isEmpty {
            // Configuration error - but we can't reject since this is not a Promise-based function
            return
        }
        
        // Configure Google Sign In with modern approach
        let gidConfig = GIDConfiguration(clientID: finalClientId)
        
        // Set additional configuration if provided
        if let webClientId = config["webClientId"] as? String {
            gidConfig.serverClientID = webClientId
        }
        
        if let hostedDomain = config["hostedDomain"] as? String {
            gidConfig.hostedDomain = hostedDomain
        }
        
        // Store default scopes
        if let scopes = config["scopes"] as? [String] {
            self.defaultScopes = scopes
        }
        
        GIDSignIn.sharedInstance.configuration = gidConfig
        self.isConfigured = true
    }
    
    // MARK: - Sign In Methods
    
    func hasPlayServices(options: [String: Any]?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        // Always return true on iOS as Google Play Services is an Android concept
        resolve(true)
    }
    
    func signIn(options: [String: Any]?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        guard isConfigured else {
            reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
            return
        }
        
        DispatchQueue.main.async {
            guard let presentingViewController = self.getCurrentViewController() else {
                reject("no_activity", "No current activity available", nil)
                return
            }
            
            // Use custom nonce if provided, otherwise generate one
            self.nonce = options?["nonce"] as? String ?? self.generateNonce()
            
            // Get scopes from options or use default scopes
            let scopes = options?["scopes"] as? [String] ?? self.defaultScopes
            
            // Use modern Google Identity approach for simple sign-ins
            if scopes.isEmpty {
                // Modern approach - use Google Identity
                GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController, hint: nil, additionalScopes: nil) { [weak self] result, error in
                    self?.handleSignInResult(result: result, error: error, resolve: resolve, reject: reject)
                }
            } else {
                // Legacy approach for scopes/OAuth features
                GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController, hint: nil, additionalScopes: scopes) { [weak self] result, error in
                    self?.handleSignInResult(result: result, error: error, resolve: resolve, reject: reject)
                }
            }
        }
    }
    
    func signInSilently(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        guard isConfigured else {
            reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
            return
        }
        
        GIDSignIn.sharedInstance.restorePreviousSignIn { [weak self] user, error in
            if let error = error {
                let nsError = error as NSError
                let code = nsError.code
                
                switch code {
                case GIDSignInError.hasNoAuthInKeychain.rawValue:
                    reject("sign_in_required", "No previous sign in found", error)
                case GIDSignInError.keychain.rawValue:
                    reject("keychain_error", "Keychain error occurred", error)
                case GIDSignInError.network.rawValue:
                    reject("network_error", "Network error occurred", error)
                case GIDSignInError.notInKeychain.rawValue:
                    reject("sign_in_required", "No previous sign in found", error)
                default:
                    reject("sign_in_error", "Silent sign in failed: \(error.localizedDescription)", error)
                }
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
    
    func addScopes(scopes: [String], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        guard isConfigured else {
            reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
            return
        }
        
        guard let currentUser = GIDSignIn.sharedInstance.currentUser else {
            resolve(NSNull())
            return
        }
        
        DispatchQueue.main.async {
            guard let presentingViewController = self.getCurrentViewController() else {
                reject("no_view_controller", "No presenting view controller available", nil)
                return
            }
            
            currentUser.addScopes(scopes, presenting: presentingViewController) { [weak self] user, error in
                if let error = error {
                    let nsError = error as NSError
                    let code = nsError.code
                    
                    switch code {
                    case GIDSignInError.canceled.rawValue:
                        reject("sign_in_cancelled", "User cancelled adding scopes", error)
                    case GIDSignInError.scopes.rawValue:
                        reject("scopes_error", "Scope error occurred", error)
                    case GIDSignInError.network.rawValue:
                        reject("network_error", "Network error occurred", error)
                    default:
                        reject("add_scopes_error", "Failed to add scopes: \(error.localizedDescription)", error)
                    }
                    return
                }
                
                guard let user = user else {
                    resolve(NSNull())
                    return
                }
                
                if let userDict = self?.convertUserToDict(user: user) {
                    resolve(userDict)
                } else {
                    resolve(NSNull())
                }
            }
        }
    }
    
    // MARK: - Sign Out Methods
    
    func signOut(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        GIDSignIn.sharedInstance.signOut()
        resolve(nil)
    }
    
    func revokeAccess(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        GIDSignIn.sharedInstance.disconnect { error in
            if let error = error {
                let nsError = error as NSError
                let code = nsError.code
                
                switch code {
                case GIDSignInError.network.rawValue:
                    reject("network_error", "Network error occurred", error)
                case GIDSignInError.unknown.rawValue:
                    reject("unknown_error", "Unknown error occurred", error)
                default:
                    reject("revoke_error", "Failed to revoke access: \(error.localizedDescription)", error)
                }
                return
            }
            resolve(nil)
        }
    }
    
    // MARK: - User State
    
    func isSignedIn(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let signedIn = GIDSignIn.sharedInstance.currentUser != nil
        resolve(signedIn)
    }
    
    func getCurrentUser(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
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
    
    func clearCachedAccessToken(accessToken: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        // iOS Google Sign In SDK doesn't have a direct equivalent to clearing cached tokens
        // The tokens are automatically managed and refreshed by the SDK
        resolve(nil)
    }
    
    func getTokens(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        guard let currentUser = GIDSignIn.sharedInstance.currentUser else {
            reject("sign_in_required", "No user is currently signed in", nil)
            return
        }
        
        currentUser.refreshTokensIfNeeded { user, error in
            if let error = error {
                let nsError = error as NSError
                let code = nsError.code
                
                switch code {
                case GIDSignInError.network.rawValue:
                    reject("network_error", "Network error occurred", error)
                case GIDSignInError.keychain.rawValue:
                    reject("keychain_error", "Keychain error occurred", error)
                default:
                    reject("token_error", "Failed to get tokens: \(error.localizedDescription)", error)
                }
                return
            }
            
            guard let user = user,
                  let accessToken = user.accessToken.tokenString else {
                reject("token_error", "No ID token available", nil)
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
    
    private func handleSignInResult(result: GIDSignInResult?, error: Error?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
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
            case GIDSignInError.hasNoAuthInKeychain.rawValue:
                reject("no_credential", "No credential available", error)
            case GIDSignInError.keychain.rawValue:
                reject("keychain_error", "Keychain error occurred", error)
            case GIDSignInError.network.rawValue:
                reject("network_error", "Network error occurred", error)
            case GIDSignInError.notInKeychain.rawValue:
                reject("not_in_keychain", "User not found in keychain", error)
            case GIDSignInError.scopes.rawValue:
                reject("scopes_error", "Scope error occurred", error)
            case GIDSignInError.userCanceled.rawValue:
                reject("sign_in_cancelled", "User cancelled the sign in", error)
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
    
    private func generateNonce() -> String {
        let random = try! SecureRandom(using: .init())
        let data = Data((0..<32).map { _ in UInt8(random.next()) })
        return data.base64EncodedString()
    }
}

 