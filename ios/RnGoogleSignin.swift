import Foundation
import GoogleSignIn
import React

@objc(RnGoogleSignin)
class RnGoogleSignin: NSObject, NativeRnGoogleSigninSpec {
    
    private var isConfigured = false
    private var defaultScopes: [String] = []
    
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
            // Throw an error if no client ID is found
            fatalError("No client ID found. Please provide iosClientId, webClientId, or ensure GoogleService-Info.plist is present.")
        }
        
        // Configure Google Sign In
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
    
    func hasPlayServices() -> Promise {
        return Promise { resolve, reject in
            // Always return true on iOS as Google Play Services is an Android concept
            resolve(true)
        }
    }
    
    func signIn(options: [String: Any]?) -> Promise {
        return Promise { resolve, reject in
            guard isConfigured else {
                reject("not_configured", "Google Sign In is not configured. Call configure() first.", nil)
                return
            }
            
            DispatchQueue.main.async {
                guard let presentingViewController = self.getCurrentViewController() else {
                    reject("no_view_controller", "No presenting view controller available", nil)
                    return
                }
                
                // Get scopes from options or use default scopes
                let scopes = options?["scopes"] as? [String] ?? self.defaultScopes
                
                GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController, hint: nil, additionalScopes: scopes.isEmpty ? nil : scopes) { [weak self] result, error in
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
                          let userDict = self?.convertUserToDict(user: result.user) else {
                        reject("sign_in_error", "Sign in failed: No user data received", nil)
                        return
                    }
                    
                    resolve(userDict)
                }
            }
        }
    }
    
    func signInSilently() -> Promise {
        return Promise { resolve, reject in
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
    }
    
    func addScopes(scopes: [String]) -> Promise {
        return Promise { resolve, reject in
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
    }
    
    // MARK: - Sign Out Methods
    
    func signOut() -> Promise {
        return Promise { resolve, reject in
            GIDSignIn.sharedInstance.signOut()
            resolve(nil)
        }
    }
    
    func revokeAccess() -> Promise {
        return Promise { resolve, reject in
            GIDSignIn.sharedInstance.disconnect { error in
                if let error = error {
                    reject("revoke_error", "Failed to revoke access: \(error.localizedDescription)", error)
                    return
                }
                resolve(nil)
            }
        }
    }
    
    // MARK: - User State
    
    func isSignedIn() -> Promise {
        return Promise { resolve, reject in
            let signedIn = GIDSignIn.sharedInstance.currentUser != nil
            resolve(signedIn)
        }
    }
    
    func getCurrentUser() -> Promise {
        return Promise { resolve, reject in
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
    }
    
    // MARK: - Utilities
    
    func clearCachedAccessToken(accessToken: String) -> Promise {
        return Promise { resolve, reject in
            // iOS Google Sign In SDK doesn't have a direct equivalent to clearing cached tokens
            // The tokens are automatically managed and refreshed by the SDK
            resolve(nil)
        }
    }
    
    func getTokens() -> Promise {
        return Promise { resolve, reject in
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
    }
    
    // MARK: - Helper Methods
    
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

// MARK: - Promise Wrapper
class Promise {
    private let resolve: RCTPromiseResolveBlock
    private let reject: RCTPromiseRejectBlock
    
    init(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        self.resolve = resolve
        self.reject = reject
    }
    
    func resolve(_ value: Any?) {
        resolve(value)
    }
    
    func reject(_ code: String, _ message: String, _ error: Error?) {
        reject(code, message, error)
    }
} 