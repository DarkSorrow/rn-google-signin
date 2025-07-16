package com.novastera.rngooglesignin

import com.google.android.gms.tasks.Task
import android.net.Uri
import android.app.Activity
import android.content.Intent
import android.os.CancellationSignal
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Base64

@ReactModule(name = RNGoogleSigninModule.NAME)
class RNGoogleSigninModule(private val reactContext: ReactApplicationContext) :
    NativeRnGoogleSigninSpec(reactContext), ActivityEventListener {

    companion object {
        const val NAME = "RnGoogleSignin"
        private const val RC_SIGN_IN = 9001
        private const val RC_ADD_SCOPES = 9002
    }

    private var googleSignInClient: GoogleSignInClient? = null
    private var credentialManager: CredentialManager? = null
    private var isConfigured = false
    private var pendingPromise: Promise? = null
    private var webClientId: String? = null
    private var defaultScopes: List<String> = emptyList()
    private var nonce: String? = null

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // MARK: - Configuration

    override fun configure(config: ReadableMap, promise: Promise) {
        try {
            webClientId = when {
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

            defaultScopes = config.getArray("scopes")?.toArrayList()?.map { it.toString() } ?: emptyList()
            val offlineAccess = config.getBoolean("offlineAccess")
            val hostedDomain = config.getString("hostedDomain")
            val forceCodeForRefreshToken = config.getBoolean("forceCodeForRefreshToken")

            // Build Google Sign In Options for OAuth (if needed for scopes or offline)
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()

            if (offlineAccess) {
                gsoBuilder.requestServerAuthCode(webClientId, forceCodeForRefreshToken)
            }

            gsoBuilder.requestIdToken(webClientId)

            // Add custom scopes
            defaultScopes.forEach { scope ->
                gsoBuilder.requestScopes(Scope(scope))
            }

            hostedDomain?.let { domain ->
                if (domain.isNotEmpty()) {
                    gsoBuilder.setHostedDomain(domain)
                }
            }

            val googleSignInOptions = gsoBuilder.build()
            googleSignInClient = GoogleSignIn.getClient(reactContext, googleSignInOptions)

            // Initialize Credential Manager for modern sign-in
            credentialManager = CredentialManager.create(reactContext)

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

        // Use custom nonce if provided, otherwise generate one
        nonce = options?.getString("nonce") ?: generateNonce()
        pendingPromise = promise

        // Use Credential Manager for modern sign-in if no custom scopes
        if (defaultScopes.isEmpty()) {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId!!)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(nonce)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = credentialManager?.getCredential(request, activity)
                    val credential = result?.credential
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
                        promise.resolve(response)
                    } else {
                        promise.reject("sign_in_error", "Sign in failed: No credential returned")
                    }
                } catch (e: GetCredentialException) {
                    if (e.type == GetCredentialException.ERROR_TYPE_USER_CANCELED) {
                        promise.reject("sign_in_cancelled", "User cancelled the sign in")
                    } else {
                        promise.reject("sign_in_error", "Sign in failed: ${e.message}", e)
                    }
                } catch (e: Exception) {
                    promise.reject("sign_in_error", "Sign in failed: ${e.message}", e)
                }
            }
        } else {
            // Fallback to legacy GoogleSignIn for scopes or offlineAccess
            pendingPromise = promise
            googleSignInClient?.let { client ->
                val signInIntent = client.signInIntent
                activity.startActivityForResult(signInIntent, RC_SIGN_IN)
            } ?: run {
                promise.reject("client_error", "Google Sign In client not initialized")
            }
        }
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

        // Attempt silent sign-in with Credential Manager if no custom scopes
        if (defaultScopes.isEmpty()) {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId!!)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .setNonce(nonce)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = credentialManager?.getCredential(request, activity)
                    val credential = result?.credential
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
                        promise.reject("sign_in_required", "No user is currently signed in")
                    }
                } catch (e: GetCredentialException) {
                    promise.reject("sign_in_required", "No user is currently signed in")
                } catch (e: Exception) {
                    promise.reject("sign_in_error", "Silent sign in failed: ${e.message}", e)
                }
            }
        } else {
            // Fallback to GoogleSignInClient
            googleSignInClient?.silentSignIn()
                ?.addOnCompleteListener { task ->
                    handleSignInResult(task, promise)
                } ?: run {
                promise.reject("client_error", "Google Sign In client not initialized")
            }
        }
    }

    override fun addScopes(scopes: ReadableArray, promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        val activity = currentActivity
        if (activity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account == null) {
            promise.resolve(null)
            return
        }

        val scopeList = scopes.toArrayList().map { Scope(it.toString()) }
        pendingPromise = promise

        GoogleSignIn.requestPermissions(
            activity,
            RC_ADD_SCOPES,
            account,
            *scopeList.toTypedArray()
        )
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
                object : CredentialManagerCallback<Void, ClearCredentialException> {
                    override fun onResult(result: Void?) {
                        // success
                    }
                    override fun onError(e: ClearCredentialException) {
                        // ignore or log
                    }
                }
            )
        }
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
        // Clear Credential Manager state
        credentialManager?.let { cm ->
            val request = ClearCredentialStateRequest()
            cm.clearCredentialStateAsync(
                request,
                null,
                Runnable::run,
                object : CredentialManagerCallback<Void, ClearCredentialException> {
                    override fun onResult(result: Void?) {
                        // success
                    }
                    override fun onError(e: ClearCredentialException) {
                        // ignore or log
                    }
                }
            )
        }
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
        // Check GoogleSignInAccount
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account != null) {
            promise.resolve(true)
            return
        }
        // Check Credential Manager
        val activity = currentActivity
        if (activity == null) {
            promise.resolve(false)
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId!!)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                credentialManager?.getCredential(request, activity)
                // If no exception, a credential exists
                promise.resolve(true)
            } catch (e: GetCredentialException) {
                promise.resolve(false)
            } catch (e: Exception) {
                promise.resolve(false)
            }
        }
    }

    override fun getCurrentUser(promise: Promise) {
        // Try GoogleSignInAccount
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account != null) {
            promise.resolve(convertAccountToMap(account))
            return
        }
        // Try Credential Manager
        val activity = currentActivity
        if (activity == null) {
            promise.resolve(null)
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId!!)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager?.getCredential(request, activity)
                val credential = result?.credential
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
    }

    // MARK: - Utilities

    override fun clearCachedAccessToken(accessToken: String, promise: Promise) {
        // Google Sign-In SDK handles token management automatically
        promise.resolve(null)
    }

    override fun getTokens(promise: Promise) {
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account == null) {
            promise.reject("sign_in_required", "No user is currently signed in")
            return
        }
        val idToken = account.idToken
        if (idToken == null) {
            promise.reject("token_error", "No ID token available")
            return
        }
        val tokens = Arguments.createMap().apply {
            putString("idToken", idToken)
            putString("accessToken", idToken)
        }
        promise.resolve(tokens)
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

    private fun generateNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
