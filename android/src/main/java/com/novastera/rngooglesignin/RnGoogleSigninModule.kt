package com.novastera.rngooglesignin

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task

@ReactModule(name = RnGoogleSigninModule.NAME)
class RnGoogleSigninModule(private val reactContext: ReactApplicationContext) :
    NativeRnGoogleSigninSpec(reactContext), ActivityEventListener {

    companion object {
        const val NAME = "RnGoogleSignin"
        private const val RC_SIGN_IN = 9001
        private const val RC_ADD_SCOPES = 9002
    }

    private var googleSignInClient: GoogleSignInClient? = null
    private var isConfigured = false
    private var pendingPromise: Promise? = null
    private var currentRequestCode: Int = 0

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // MARK: - Configuration

    override fun configure(config: ReadableMap, promise: Promise) {
        try {
            val webClientId = when {
                config.hasKey("webClientId") -> config.getString("webClientId")
                config.hasKey("androidClientId") -> config.getString("androidClientId")
                else -> {
                    promise.reject("configuration_error", "webClientId or androidClientId is required")
                    return
                }
            }

            if (webClientId.isNullOrEmpty()) {
                promise.reject("configuration_error", "Client ID cannot be empty")
                return
            }

            val scopes = config.getArray("scopes")?.toArrayList()?.map { it.toString() } ?: emptyList()
            val offlineAccess = config.getBoolean("offlineAccess")
            val hostedDomain = config.getString("hostedDomain")
            val forceCodeForRefreshToken = config.getBoolean("forceCodeForRefreshToken")

            // Build Google Sign In Options
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()

            if (offlineAccess) {
                gsoBuilder.requestServerAuthCode(webClientId, forceCodeForRefreshToken)
            }

            gsoBuilder.requestIdToken(webClientId)

            // Add custom scopes
            scopes.forEach { scope ->
                gsoBuilder.requestScopes(com.google.android.gms.common.api.Scope(scope))
            }

            hostedDomain?.let { domain ->
                gsoBuilder.setHostedDomain(domain)
            }

            val googleSignInOptions = gsoBuilder.build()
            googleSignInClient = GoogleSignIn.getClient(reactContext, googleSignInOptions)

            isConfigured = true
            promise.resolve(null)

        } catch (e: Exception) {
            promise.reject("configuration_error", "Failed to configure Google Sign In: ${e.message}", e)
        }
    }

    // MARK: - Sign In Methods

    override fun hasPlayServices(options: ReadableMap?, promise: Promise) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(reactContext)
        promise.resolve(result == ConnectionResult.SUCCESS)
    }

    override fun signIn(options: ReadableMap?, promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        val activity = getCurrentActivity()
        if (activity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

        pendingPromise = promise
        currentRequestCode = RC_SIGN_IN

        googleSignInClient?.let { client ->
            val signInIntent = client.signInIntent
            activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        } ?: run {
            promise.reject("client_error", "Google Sign In client not initialized")
        }
    }

    override fun signInSilently(promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        googleSignInClient?.let { client ->
            client.silentSignIn()
                .addOnCompleteListener { task ->
                    handleSignInResult(task, promise)
                }
        } ?: run {
            promise.reject("client_error", "Google Sign In client not initialized")
        }
    }

    override fun addScopes(scopes: ReadableArray, promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        val activity = getCurrentActivity()
        if (activity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account == null) {
            promise.reject("sign_in_required", "No user is currently signed in")
            return
        }

        val scopeList = scopes.toArrayList().map { com.google.android.gms.common.api.Scope(it.toString()) }
        
        pendingPromise = promise
        currentRequestCode = RC_ADD_SCOPES

        GoogleSignIn.requestPermissions(
            activity,
            RC_ADD_SCOPES,
            account,
            *scopeList.toTypedArray()
        )
    }

    // MARK: - Sign Out Methods

    override fun signOut(promise: Promise) {
        googleSignInClient?.let { client ->
            client.signOut()
                .addOnCompleteListener {
                    promise.resolve(null)
                }
        } ?: run {
            promise.resolve(null)
        }
    }

    override fun revokeAccess(promise: Promise) {
        googleSignInClient?.let { client ->
            client.revokeAccess()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        promise.resolve(null)
                    } else {
                        promise.reject("revoke_error", "Failed to revoke access: ${task.exception?.message}")
                    }
                }
        } ?: run {
            promise.resolve(null)
        }
    }

    // MARK: - User State

    override fun isSignedIn(promise: Promise) {
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        promise.resolve(account != null)
    }

    override fun getCurrentUser(promise: Promise) {
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account != null) {
            promise.resolve(convertAccountToMap(account))
        } else {
            promise.resolve(null)
        }
    }

    // MARK: - Utilities

    override fun clearCachedAccessToken(accessToken: String, promise: Promise) {
        // Android doesn't require explicit token clearing as it's handled by the Google APIs
        promise.resolve(null)
    }

    override fun getTokens(promise: Promise) {
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account == null) {
            promise.reject("sign_in_required", "No user is currently signed in")
            return
        }

        // Get fresh token
        GoogleSignIn.getClient(reactContext, GoogleSignInOptions.DEFAULT_SIGN_IN)
            .getAccessToken(account)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val tokens = Arguments.createMap().apply {
                        putString("accessToken", task.result?.token)
                        putString("idToken", account.idToken)
                    }
                    promise.resolve(tokens)
                } else {
                    promise.reject("token_error", "Failed to get tokens: ${task.exception?.message}")
                }
            }
    }

    // MARK: - Activity Event Listener

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                handleSignInResult(task, pendingPromise)
                pendingPromise = null
            }
            RC_ADD_SCOPES -> {
                if (resultCode == Activity.RESULT_OK) {
                    val account = GoogleSignIn.getLastSignedInAccount(reactContext)
                    if (account != null) {
                        pendingPromise?.resolve(convertAccountToMap(account))
                    } else {
                        pendingPromise?.reject("add_scopes_error", "Failed to add scopes: No account found")
                    }
                } else {
                    pendingPromise?.reject("add_scopes_cancelled", "User cancelled adding scopes")
                }
                pendingPromise = null
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // Not needed for Google Sign In
    }

    // MARK: - Helper Methods

    private fun handleSignInResult(task: Task<GoogleSignInAccount>, promise: Promise?) {
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                promise?.resolve(convertAccountToMap(account))
            } else {
                promise?.reject("sign_in_error", "Sign in failed: No account data received")
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            when (e.statusCode) {
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                    promise?.reject("sign_in_cancelled", "User cancelled the sign in")
                }
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> {
                    promise?.reject("in_progress", "Sign in is already in progress")
                }
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                    promise?.reject("sign_in_failed", "Sign in failed")
                }
                else -> {
                    promise?.reject("sign_in_error", "Sign in failed with code: ${e.statusCode}")
                }
            }
        } catch (e: Exception) {
            promise?.reject("sign_in_error", "Sign in failed: ${e.message}", e)
        }
    }

    private fun convertAccountToMap(account: GoogleSignInAccount): WritableMap {
        val userInfo = Arguments.createMap().apply {
            putString("id", account.id ?: "")
            putString("name", account.displayName)
            putString("email", account.email ?: "")
            putString("photo", account.photoUrl?.toString())
            putString("familyName", account.familyName)
            putString("givenName", account.givenName)
        }

        val scopes = Arguments.createArray().apply {
            account.grantedScopes?.forEach { scope ->
                pushString(scope.scopeUri)
            }
        }

        return Arguments.createMap().apply {
            putMap("user", userInfo)
            putArray("scopes", scopes)
            putString("serverAuthCode", account.serverAuthCode)
            putString("idToken", account.idToken)
        }
    }
}
