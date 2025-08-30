package com.novastera.rngooglesignin

import android.app.Activity
import android.content.Intent
import android.os.Build
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

    // Don't store a single instance - create it dynamically
    private var isConfigured = false
    private var currentPromise: Promise? = null
    private var webClientId: String? = null
    private var nonce: String? = null

    // Use a single executor for all async operations to avoid memory leaks
    private val mainExecutor = Executors.newSingleThreadExecutor()

    // Use WeakReference to prevent memory leaks
    private val weakReactContext = WeakReference(reactContext)

    // Cache for CredentialManager to avoid recreating unnecessarily
    private var cachedCredentialManager: CredentialManager? = null
    private var cachedForActivity: Activity? = null

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // MARK: - Helper Methods

    /**
     * Gets or creates a CredentialManager instance with the appropriate context.
     * This ensures compatibility across different Android versions.
     */
    private fun getOrCreateCredentialManager(activity: Activity?): CredentialManager? {
        // If we have a valid activity, always prefer using it
        if (activity != null) {
            // Check if we need to recreate the manager (different activity or not cached)
            if (cachedCredentialManager == null || cachedForActivity != activity) {
                try {
                    // Create with activity context for better compatibility
                    cachedCredentialManager = CredentialManager.create(activity)
                    cachedForActivity = activity
                } catch (e: Exception) {
                    // Fallback to application context if activity context fails
                    try {
                        cachedCredentialManager = CredentialManager.create(reactContext)
                        cachedForActivity = null
                    } catch (e2: Exception) {
                        return null
                    }
                }
            }
        } else if (cachedCredentialManager == null) {
            // No activity available, try with application context
            try {
                cachedCredentialManager = CredentialManager.create(reactContext)
                cachedForActivity = null
            } catch (e: Exception) {
                return null
            }
        }

        return cachedCredentialManager
    }

    /**
     * Validates that the current activity is available and in a valid state
     * @return The current activity if valid, null otherwise
     */
    private fun getValidActivity(): Activity? {
        val activity = currentActivity ?: return null

        // Check if activity is in a valid state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (activity.isDestroyed) {
                return null
            }
        }

        if (activity.isFinishing) {
            return null
        }

        return activity
    }

    /**
     * Verifies Google Play Services availability
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(reactContext)
        return resultCode == ConnectionResult.SUCCESS
    }

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

            // Don't create CredentialManager here - create it dynamically when needed
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
                val activity = getValidActivity()
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

        // Enhanced Activity validation
        val activity = getValidActivity()
        if (activity == null) {
            promise.reject("no_valid_activity", "No valid activity available for sign in")
            return
        }

        // Check Google Play Services availability
        if (!isGooglePlayServicesAvailable()) {
            promise.reject("play_services_error", "Google Play Services not available. Please ensure Google Play Services is installed and up to date.")
            return
        }

        // Get or create CredentialManager with activity context
        val credentialManager = getOrCreateCredentialManager(activity)
        if (credentialManager == null) {
            promise.reject("credential_manager_error", "Failed to initialize Credential Manager")
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

        // Use activity context for the operation
        credentialManager?.getCredentialAsync(
            request = request,
            context = activity,  // Always use activity context
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
                    val errorCode = when {
                        e.message?.contains("cancel", ignoreCase = true) == true -> "sign_in_cancelled"
                        e.message?.contains("no credential", ignoreCase = true) == true -> "no_credential"
                        e.message?.contains("network", ignoreCase = true) == true -> "network_error"
                        e.message?.contains("selector UI", ignoreCase = true) == true -> "ui_error"
                        else -> "sign_in_error"
                    }

                    val errorMessage = when (errorCode) {
                        "sign_in_cancelled" -> "User cancelled the sign in"
                        "no_credential" -> "No Google account found. Please add a Google account in Settings > Accounts"
                        "network_error" -> "Network error during sign in"
                        "ui_error" -> "Failed to launch selector UI. Please try again"
                        else -> "Sign in failed: ${e.message}"
                    }

                    currentPromise?.reject(errorCode, errorMessage, e)
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

        // Enhanced Activity validation
        val activity = getValidActivity()
        if (activity == null) {
            promise.reject("no_valid_activity", "No valid activity available for silent sign in")
            return
        }

        // Get or create CredentialManager with activity context
        val credentialManager = getOrCreateCredentialManager(activity)
        if (credentialManager == null) {
            promise.reject("credential_manager_error", "Failed to initialize Credential Manager")
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

        // Use activity context for the operation
        credentialManager?.getCredentialAsync(
            request = request,
            context = activity,  // Always use activity context
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
                    when {
                        e.message?.contains("selector UI", ignoreCase = true) == true -> {
                            currentPromise?.reject("ui_error", "Failed to launch selector UI")
                        }
                        else -> {
                            currentPromise?.reject("sign_in_required", "No user is currently signed in")
                        }
                    }
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
        val activity = getValidActivity()
        val credentialManager = if (activity != null) {
            getOrCreateCredentialManager(activity)
        } else {
            // Try with cached manager or create with app context
            cachedCredentialManager ?: try {
                CredentialManager.create(reactContext)
            } catch (e: Exception) {
                null
            }
        }

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

        // Clear cached manager on sign out
        cachedCredentialManager = null
        cachedForActivity = null

        promise.resolve(null)
    }

    override fun revokeAccess(promise: Promise) {
        // Clear Credential Manager state
        val activity = getValidActivity()
        val credentialManager = if (activity != null) {
            getOrCreateCredentialManager(activity)
        } else {
            // Try with cached manager or create with app context
            cachedCredentialManager ?: try {
                CredentialManager.create(reactContext)
            } catch (e: Exception) {
                null
            }
        }

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

        // Clear cached manager on revoke
        cachedCredentialManager = null
        cachedForActivity = null

        promise.resolve(null)
    }

    // MARK: - User State

    override fun isSignedIn(promise: Promise) {
        // Check if we have any configured client ID
        val hasClientId = webClientId != null
        if (!hasClientId) {
            promise.resolve(false)
            return
        }

        // Check if we have a valid activity
        val activity = getValidActivity()
        if (activity == null) {
            promise.resolve(false)
            return
        }

        // Get or create CredentialManager
        val credentialManager = getOrCreateCredentialManager(activity)
        if (credentialManager == null) {
            promise.resolve(false)
            return
        }

        // Try to get existing credentials without triggering UI
        // Use setAutoSelectEnabled(false) to prevent automatic credential selection
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId!!)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(false)  // This prevents automatic UI triggering
            .build()
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        credentialManager.getCredentialAsync(
            request = request,
            context = activity,
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    // If we get here, there are existing credentials
                    promise.resolve(true)
                }

                override fun onError(e: GetCredentialException) {
                    // No existing credentials or error occurred
                    promise.resolve(false)
                }
            }
        )
    }

    override fun getCurrentUser(promise: Promise) {
        // Try Credential Manager
        val activity = getValidActivity()
        if (activity == null) {
            promise.resolve(null)
            return
        }

        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.resolve(null)
            return
        }

        // Get or create CredentialManager with activity context
        val credentialManager = getOrCreateCredentialManager(activity)
        if (credentialManager == null) {
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

        // Use activity context for the operation
        credentialManager?.getCredentialAsync(
            request = request,
            context = activity,  // Always use activity context
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
        val activity = getValidActivity()
        if (activity == null) {
            promise.reject("no_valid_activity", "No valid activity available")
            return
        }

        val currentWebClientId = webClientId
        if (currentWebClientId == null) {
            promise.reject("not_configured", "Google Sign In is not configured")
            return
        }

        // Get or create CredentialManager with activity context
        val credentialManager = getOrCreateCredentialManager(activity)
        if (credentialManager == null) {
            promise.reject("credential_manager_error", "Failed to initialize Credential Manager")
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

        // Use activity context for the operation
        credentialManager?.getCredentialAsync(
            request = request,
            context = activity,  // Always use activity context
            cancellationSignal = null,
            executor = mainExecutor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    try {
                        val credential = result.credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            // Return only tokens as per TypeScript interface
                            val tokens = Arguments.createMap().apply {
                                putString("idToken", idTokenCredential.idToken)
                                // Note: Credential Manager doesn't provide separate access tokens
                                // so we return the idToken as accessToken for compatibility
                                putString("accessToken", idTokenCredential.idToken)
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
                    when {
                        e.message?.contains("selector UI", ignoreCase = true) == true -> {
                            promise.reject("ui_error", "Failed to launch selector UI")
                        }
                        else -> {
                            promise.reject("sign_in_required", "No user is currently signed in")
                        }
                    }
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
