package com.novastera.rngooglesignin

import android.app.Activity
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors
import java.lang.ref.WeakReference

@ReactModule(name = RNGoogleSigninModule.NAME)
class RNGoogleSigninModule(private val reactContext: ReactApplicationContext) :
    NativeRnGoogleSigninSpec(reactContext), ActivityEventListener {

    companion object {
        const val NAME = "RnGoogleSignin"
    }

    private var credentialManager: CredentialManager? = null
    private var isConfigured = false
    private var currentPromise: Promise? = null
    private var webClientId: String? = null
    private var nonce: String? = null
    
    // Use a single executor for all async operations to avoid memory leaks
    private val mainExecutor = Executors.newSingleThreadExecutor()
    
    // Use WeakReference to prevent memory leaks
    private val weakReactContext = WeakReference(reactContext)

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // MARK: - Configuration

    override fun configure(config: ReadableMap) {
        try {
            webClientId = when {
                config.hasKey("webClientId") -> config.getString("webClientId")
                config.hasKey("androidClientId") -> config.getString("androidClientId")
                else -> {
                    // Configuration error - but we can't reject since this is not a Promise-based function
                    // The error will be handled when trying to use the module
                    return
                }
            }

            if (webClientId.isNullOrEmpty()) {
                // Configuration error - but we can't reject since this is not a Promise-based function
                return
            }

            // Initialize Credential Manager for modern sign-in
            credentialManager = CredentialManager.create(reactContext)

            isConfigured = true

        } catch (e: Exception) {
            // Configuration error - but we can't reject since this is not a Promise-based function
            // The error will be handled when trying to use the module
        }
    }

    // MARK: - Sign In Methods

    override fun hasPlayServices(options: ReadableMap?, promise: Promise) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(reactContext)

        if (result != ConnectionResult.SUCCESS) {
            val showPlayServicesUpdateDialog = options?.getBoolean("showPlayServicesUpdateDialog") ?: false
            if (showPlayServicesUpdateDialog && googleApiAvailability.isUserResolvableError(result)) {
                val activity = currentActivity
                activity?.let {
                    val requestCode = 2404
                    googleApiAvailability.getErrorDialog(it, result, requestCode)?.show()
                }
            }
            promise.reject("play_services_not_available", "Play services not available")
        } else {
            promise.resolve(true)
        }
    }

    override fun signIn(options: ReadableMap?, promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

        // Clear any existing promise to prevent conflicts
        currentPromise?.reject("cancelled", "Previous operation was cancelled by new request")
        currentPromise = promise

        // Use custom nonce if provided, otherwise generate one
        nonce = options?.getString("nonce") ?: generateNonce()

        // Use Credential Manager for modern sign-in
        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(currentWebClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Use the async version with callback instead of suspend function
        credentialManager?.getCredentialAsync(
            request = request,
            context = reactContext,
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    try {
                        val credential = result.credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val response = Arguments.createMap().apply {
                                val userInfo = Arguments.createMap().apply {
                                    putString("id", idTokenCredential.id ?: "")
                                    putString("name", idTokenCredential.displayName)
                                    putString("email", idTokenCredential.id)
                                    putString("photo", idTokenCredential.profilePictureUri?.toString())
                                    putString("familyName", idTokenCredential.familyName)
                                    putString("givenName", idTokenCredential.givenName)
                                }
                                val scopesArray = Arguments.createArray()
                                // Credential Manager does not provide scopes info

                                putMap("user", userInfo)
                                putArray("scopes", scopesArray)
                                putString("serverAuthCode", null)
                                putString("idToken", idTokenCredential.idToken)
                            }
                            currentPromise?.resolve(response)
                        } else {
                            currentPromise?.reject("sign_in_error", "Sign in failed: No credential returned")
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        currentPromise?.reject("parsing_error", "Failed to parse Google ID token: ${e.message}", e)
                    } catch (e: Exception) {
                        currentPromise?.reject("sign_in_error", "Sign in failed: ${e.message}", e)
                    } finally {
                        currentPromise = null
                    }
                }

                override fun onError(e: GetCredentialException) {
                    // Handle different types of credential exceptions
                    when {
                        e.message?.contains("cancel", ignoreCase = true) == true -> {
                            currentPromise?.reject("sign_in_cancelled", "User cancelled the sign in")
                        }
                        e.message?.contains("no credential", ignoreCase = true) == true -> {
                            currentPromise?.reject("no_credential", "No credential available")
                        }
                        e.message?.contains("network", ignoreCase = true) == true -> {
                            currentPromise?.reject("network_error", "Network error during sign in")
                        }
                        else -> {
                            currentPromise?.reject("sign_in_error", "Sign in failed: ${e.message}", e)
                        }
                    }
                    currentPromise = null
                }
            }
        )
    }

    override fun signInSilently(promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

        // Clear any existing promise to prevent conflicts
        currentPromise?.reject("cancelled", "Previous operation was cancelled by new request")
        currentPromise = promise

        // Attempt silent sign-in with Credential Manager
        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(currentWebClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Use the async version with callback instead of suspend function
        credentialManager?.getCredentialAsync(
            request = request,
            context = reactContext,
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    try {
                        val credential = result.credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val response = Arguments.createMap().apply {
                                val userInfo = Arguments.createMap().apply {
                                    putString("id", idTokenCredential.id ?: "")
                                    putString("name", idTokenCredential.displayName)
                                    putString("email", idTokenCredential.id)
                                    putString("photo", idTokenCredential.profilePictureUri?.toString())
                                    putString("familyName", idTokenCredential.familyName)
                                    putString("givenName", idTokenCredential.givenName)
                                }
                                val scopesArray = Arguments.createArray()
                                putMap("user", userInfo)
                                putArray("scopes", scopesArray)
                                putString("serverAuthCode", null)
                                putString("idToken", idTokenCredential.idToken)
                            }
                            currentPromise?.resolve(response)
                        } else {
                            currentPromise?.reject("sign_in_required", "No user is currently signed in")
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        currentPromise?.reject("parsing_error", "Failed to parse Google ID token: ${e.message}", e)
                    } catch (e: Exception) {
                        currentPromise?.reject("sign_in_error", "Silent sign in failed: ${e.message}", e)
                    } finally {
                        currentPromise = null
                    }
                }

                override fun onError(e: GetCredentialException) {
                    currentPromise?.reject("sign_in_required", "No user is currently signed in")
                    currentPromise = null
                }
            }
        )
    }

    override fun addScopes(scopes: ReadableArray, promise: Promise) {
        // Modern Credential Manager doesn't support custom scopes
        // This is a limitation of the modern approach
        promise.reject("not_supported", "Custom scopes are not supported in modern Google Sign In. Use basic sign-in only.")
    }

    // MARK: - Sign Out Methods

    override fun signOut(promise: Promise) {
        // Clear Credential Manager state
        credentialManager?.let { cm ->
            val request = ClearCredentialStateRequest()
            cm.clearCredentialStateAsync(
                request,
                null,
                Runnable::run,
                object : CredentialManagerCallback<Void?, ClearCredentialException> {
                    override fun onResult(result: Void?) {
                        // success
                    }
                    override fun onError(e: ClearCredentialException) {
                        // ignore or log
                    }
                }
            )
        }
        promise.resolve(null)
    }

    override fun revokeAccess(promise: Promise) {
        // Clear Credential Manager state
        credentialManager?.let { cm ->
            val request = ClearCredentialStateRequest()
            cm.clearCredentialStateAsync(
                request,
                null,
                Runnable::run,
                object : CredentialManagerCallback<Void?, ClearCredentialException> {
                    override fun onResult(result: Void?) {
                        // success
                    }
                    override fun onError(e: ClearCredentialException) {
                        // ignore or log
                    }
                }
            )
        }
        promise.resolve(null)
    }

    // MARK: - User State

    override fun isSignedIn(promise: Promise) {
        // Check Credential Manager
        val activity = currentActivity
        if (activity == null) {
            promise.resolve(false)
            return
        }
        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.resolve(false)
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(currentWebClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Use the async version with callback
        credentialManager?.getCredentialAsync(
            request = request,
            context = reactContext,
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    // If no exception, a credential exists
                    promise.resolve(true)
                }

                override fun onError(e: GetCredentialException) {
                    promise.resolve(false)
                }
            }
        )
    }

    override fun getCurrentUser(promise: Promise) {
        // Try Credential Manager
        val activity = currentActivity
        if (activity == null) {
            promise.resolve(null)
            return
        }
        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.resolve(null)
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(currentWebClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Use the async version with callback
        credentialManager?.getCredentialAsync(
            request = request,
            context = reactContext,
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    try {
                        val credential = result.credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val response = Arguments.createMap().apply {
                                val userInfo = Arguments.createMap().apply {
                                    putString("id", idTokenCredential.id ?: "")
                                    putString("name", idTokenCredential.displayName)
                                    putString("email", idTokenCredential.id)
                                    putString("photo", idTokenCredential.profilePictureUri?.toString())
                                    putString("familyName", idTokenCredential.familyName)
                                    putString("givenName", idTokenCredential.givenName)
                                }
                                val scopesArray = Arguments.createArray()
                                putMap("user", userInfo)
                                putArray("scopes", scopesArray)
                                putString("serverAuthCode", null)
                                putString("idToken", idTokenCredential.idToken)
                            }
                            promise.resolve(response)
                        } else {
                            promise.resolve(null)
                        }
                    } catch (e: Exception) {
                        promise.resolve(null)
                    }
                }

                override fun onError(e: GetCredentialException) {
                    promise.resolve(null)
                }
            }
        )
    }

    // MARK: - Utilities

    override fun clearCachedAccessToken(accessToken: String, promise: Promise) {
        // Modern approach doesn't cache access tokens
        promise.resolve(null)
    }

    override fun getTokens(promise: Promise) {
        // For modern approach, we get tokens from Credential Manager
        val activity = currentActivity
        if (activity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }
        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.reject("not_configured", "Google Sign In is not configured")
            return
        }
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(currentWebClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Use the async version with callback
        credentialManager?.getCredentialAsync(
            request = request,
            context = reactContext,
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    try {
                        val credential = result.credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val tokens = Arguments.createMap().apply {
                                putString("idToken", idTokenCredential.idToken)
                                putString("accessToken", idTokenCredential.idToken) // Note: Credential Manager doesn't provide access tokens
                            }
                            promise.resolve(tokens)
                        } else {
                            promise.reject("sign_in_required", "No user is currently signed in")
                        }
                    } catch (e: Exception) {
                        promise.reject("sign_in_required", "No user is currently signed in")
                    }
                }

                override fun onError(e: GetCredentialException) {
                    promise.reject("sign_in_required", "No user is currently signed in")
                }
            }
        )
    }

    // MARK: - Activity Event Listener

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, intent: Intent?) {
        // Not needed for modern Credential Manager approach
    }

    override fun onNewIntent(intent: Intent?) {
        // Not needed for Google Sign In
    }

    // MARK: - Helper Methods

    private fun generateNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
