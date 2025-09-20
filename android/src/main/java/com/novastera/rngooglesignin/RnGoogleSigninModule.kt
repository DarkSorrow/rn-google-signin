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
import java.util.concurrent.atomic.AtomicReference
import java.lang.ref.WeakReference

@ReactModule(name = RNGoogleSigninModule.NAME)
class RNGoogleSigninModule(private val reactContext: ReactApplicationContext) :
    NativeRnGoogleSigninSpec(reactContext), ActivityEventListener {

    companion object {
        const val NAME = "RnGoogleSignin"
        private const val NONCE_LENGTH = 32
    }

    // Thread-safe configuration state
    @Volatile
    private var isConfigured = false
    
    // Thread-safe promise management
    private val currentPromiseRef = AtomicReference<Promise?>(null)
    
    // Thread-safe client ID storage
    @Volatile
    private var webClientId: String? = null
    
    // Thread-safe nonce storage
    @Volatile
    private var nonce: String? = null

    // Single executor for all async operations
    private val mainExecutor = Executors.newSingleThreadExecutor()

    // Weak reference to prevent memory leaks
    private val weakReactContext = WeakReference(reactContext)

    // Thread-safe CredentialManager cache
    private val credentialManagerRef = AtomicReference<CredentialManager?>(null)
    private val cachedActivityRef = AtomicReference<Activity?>(null)

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // MARK: - Helper Methods

    /**
     * Creates a standardized user response map from GoogleIdTokenCredential
     */
    private fun createUserResponse(idTokenCredential: GoogleIdTokenCredential): WritableMap {
        return Arguments.createMap().apply {
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
    }

    /**
     * Creates a standardized tokens response map
     */
    private fun createTokensResponse(idTokenCredential: GoogleIdTokenCredential): WritableMap {
        return Arguments.createMap().apply {
            putString("idToken", idTokenCredential.idToken)
            // Note: Credential Manager doesn't provide separate access tokens
            // so we return the idToken as accessToken for compatibility
            putString("accessToken", idTokenCredential.idToken)
        }
    }

    /**
     * Gets or creates a CredentialManager instance with the appropriate context.
     * Optimized for thread safety and reduced object creation.
     */
    private fun getOrCreateCredentialManager(activity: Activity?): CredentialManager? {
        val currentActivity = cachedActivityRef.get()
        val currentManager = credentialManagerRef.get()
        
        // Return cached manager if activity matches and manager exists
        if (currentManager != null && currentActivity == activity) {
            return currentManager
        }
        
        // Create new manager with appropriate context
        val newManager = try {
            when {
                activity != null -> CredentialManager.create(activity)
                else -> CredentialManager.create(reactContext)
            }
        } catch (e: Exception) {
            // Fallback to application context if activity context fails
            try {
                CredentialManager.create(reactContext)
            } catch (e2: Exception) {
                return null
            }
        }
        
        // Update cache atomically
        credentialManagerRef.set(newManager)
        cachedActivityRef.set(activity)
        
        return newManager
    }

    /**
     * Validates that the current activity is available and in a valid state.
     * Optimized with early returns and reduced API calls.
     */
    private fun getValidActivity(): Activity? {
        val activity = reactContext.getCurrentActivity() ?: return null
        
        // Early return for invalid states
        if (activity.isFinishing) return null
        
        // Check destruction state only on supported API levels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            return null
        }
        
        return activity
    }

    /**
     * Verifies Google Play Services availability.
     * Cached for better performance.
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(reactContext) == ConnectionResult.SUCCESS
    }

    /**
     * Validates sign-in prerequisites and returns validated objects or null if validation fails
     */
    private fun validateSignInPrerequisites(): Pair<Activity, CredentialManager>? {
        if (!isConfigured) {
            return null
        }

        val activity = getValidActivity() ?: return null
        if (!isGooglePlayServicesAvailable()) {
            return null
        }

        val credentialManager = getOrCreateCredentialManager(activity) ?: return null
        return activity to credentialManager
    }

    /**
     * Clears credential state and cached resources
     */
    private fun clearCredentialState() {
        val activity = getValidActivity()
        val credentialManager = if (activity != null) {
            getOrCreateCredentialManager(activity)
        } else {
            credentialManagerRef.get() ?: try {
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

        // Clear cached resources
        credentialManagerRef.set(null)
        cachedActivityRef.set(null)
    }

    // MARK: - Configuration

    /**
     * Attempts to get client ID from google-services.json automatically.
     * Uses Android's automatic resource generation - no manual parsing.
     * Prioritizes Web Client ID for Credential Manager, falls back to Android Client ID.
     * Returns null if not available - won't crash.
     */
    private fun getClientIdFromGoogleServices(): String? {
        return try {
            // Priority 1: Try to get Web Client ID (preferred for Credential Manager)
            val webClientId = tryGetResource("default_web_client_id")
            if (webClientId != null) return webClientId
            
            // Priority 2: Try alternative web client ID names
            val altWebClientId = tryGetResource("web_client_id")
            if (altWebClientId != null) return altWebClientId
            
            // Priority 3: Fall back to Android Client ID
            val androidClientId = tryGetResource("default_android_client_id")
            if (androidClientId != null) return androidClientId
            
            // Priority 4: Try alternative android client ID names
            val altAndroidClientId = tryGetResource("android_client_id")
            if (altAndroidClientId != null) return altAndroidClientId
            
            // No valid client ID found
            null
        } catch (e: Exception) {
            // Any error - return null safely
            null
        }
    }
    
    /**
     * Helper method to safely try to get a resource by name
     */
    private fun tryGetResource(resourceName: String): String? {
        return try {
            val resourceId = reactContext.resources.getIdentifier(
                resourceName, "string", reactContext.packageName
            )
            if (resourceId != 0) {
                val clientId = reactContext.getString(resourceId)
                if (clientId.isNotEmpty() && clientId != "null") {
                    clientId
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun configure(config: ReadableMap) {
        try {
            webClientId = when {
                // Priority 1: Manual configuration (user override)
                config.hasKey("androidClientId") -> config.getString("androidClientId")
                config.hasKey("webClientId") -> config.getString("webClientId")
                else -> {
                    // Priority 2: Automatic detection from google-services.json as fallback
                    getClientIdFromGoogleServices()
                }
            }

            if (webClientId.isNullOrEmpty()) {
                // Configuration error - but we can't reject since this is not a Promise-based function
                // The error will be handled when trying to use the module
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
        // Validate prerequisites
        val validationResult = validateSignInPrerequisites()
        if (validationResult == null) {
            promise.reject("not_configured", "Google Sign In is not configured or prerequisites not met. Call configure() first.")
            return
        }

        val (activity, credentialManager) = validationResult

        // Clear any existing promise to prevent conflicts
        currentPromiseRef.getAndSet(promise)?.reject("cancelled", "Previous operation was cancelled by new request")

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
                            val response = createUserResponse(idTokenCredential)
                            currentPromiseRef.getAndSet(null)?.resolve(response)
                        } else {
                            currentPromiseRef.getAndSet(null)?.reject("sign_in_error", "Sign in failed: No credential returned")
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        currentPromiseRef.getAndSet(null)?.reject("parsing_error", "Failed to parse Google ID token: ${e.message}", e)
                    } catch (e: Exception) {
                        currentPromiseRef.getAndSet(null)?.reject("sign_in_error", "Sign in failed: ${e.message}", e)
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

                    currentPromiseRef.getAndSet(null)?.reject(errorCode, errorMessage, e)
                }
            }
        )
    }

    override fun signInSilently(promise: Promise) {
        // Validate prerequisites
        val validationResult = validateSignInPrerequisites()
        if (validationResult == null) {
            promise.reject("not_configured", "Google Sign In is not configured or prerequisites not met. Call configure() first.")
            return
        }

        val (activity, credentialManager) = validationResult

        // Clear any existing promise to prevent conflicts
        currentPromiseRef.getAndSet(promise)?.reject("cancelled", "Previous operation was cancelled by new request")

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
                            val response = createUserResponse(idTokenCredential)
                            currentPromiseRef.getAndSet(null)?.resolve(response)
                        } else {
                            currentPromiseRef.getAndSet(null)?.reject("sign_in_required", "No user is currently signed in")
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        currentPromiseRef.getAndSet(null)?.reject("parsing_error", "Failed to parse Google ID token: ${e.message}", e)
                    } catch (e: Exception) {
                        currentPromiseRef.getAndSet(null)?.reject("sign_in_error", "Silent sign in failed: ${e.message}", e)
                    }
                }

                override fun onError(e: GetCredentialException) {
                    when {
                        e.message?.contains("selector UI", ignoreCase = true) == true -> {
                            currentPromiseRef.getAndSet(null)?.reject("ui_error", "Failed to launch selector UI")
                        }
                        else -> {
                            currentPromiseRef.getAndSet(null)?.reject("sign_in_required", "No user is currently signed in")
                        }
                    }
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
        clearCredentialState()
        promise.resolve(null)
    }

    override fun revokeAccess(promise: Promise) {
        clearCredentialState()
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
                            val response = createUserResponse(idTokenCredential)
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
                            val tokens = createTokensResponse(idTokenCredential)
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

    // Implement the exact signatures required by Expo SDK 54
    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        // Not needed for modern Credential Manager approach
    }

    override fun onNewIntent(intent: Intent) {
        // Not needed for Google Sign In
    }

    // MARK: - Helper Methods

    private fun generateNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(NONCE_LENGTH)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Cleanup method for module lifecycle management
     */
    fun cleanup() {
        // Cancel any pending operations
        currentPromiseRef.getAndSet(null)?.reject("cancelled", "Module cleanup")
        
        // Clear cached resources
        credentialManagerRef.set(null)
        cachedActivityRef.set(null)
        
        // Shutdown executor
        mainExecutor.shutdown()
    }
}
