package com.novastera.rngooglesignin

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.UiThreadUtil
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ReactModule(name = RnGoogleSigninModule.NAME)
class RnGoogleSigninModule(private val reactContext: ReactApplicationContext) :
    NativeRnGoogleSigninSpec(reactContext), ActivityEventListener {

    companion object {
        const val NAME = "RnGoogleSignin"
        private const val RC_SIGN_IN = 9001
        private const val REQUEST_CODE_ADD_SCOPES = 53295
        private const val REQUEST_CODE_RECOVER_AUTH = 53294
        private const val PLAY_SERVICES_NOT_AVAILABLE = "PLAY_SERVICES_NOT_AVAILABLE"
        private const val SHOULD_RECOVER = "SHOULD_RECOVER"
        private const val ASYNC_OP_IN_PROGRESS = "ASYNC_OP_IN_PROGRESS"
    }

    private var googleSignInClient: GoogleSignInClient? = null
    private var isConfigured = false
    private var pendingPromise: Promise? = null
    private var currentRequestCode: Int = 0
    private var pendingAuthRecovery: PendingAuthRecovery? = null

    private val signInOrAddScopesPromiseWrapper = PromiseWrapper(NAME)
    private val silentSignInPromiseWrapper = PromiseWrapper(NAME)
    private val tokenRetrievalPromiseWrapper = PromiseWrapper(NAME)
    private val tokenClearingPromiseWrapper = PromiseWrapper(NAME)

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // MARK: - Configuration

    override fun configure(config: ReadableMap) {
        val webClientId = when {
            config.hasKey("webClientId") -> config.getString("webClientId")
            config.hasKey("androidClientId") -> config.getString("androidClientId")
            else -> {
                // Use empty string as fallback - this will cause configuration to fail gracefully
                ""
            }
        }

        if (webClientId.isNullOrEmpty()) {
            // Configuration failed but we don't throw - just mark as not configured
            isConfigured = false
            return
        }

        val scopes = config.getArray("scopes")?.toArrayList()?.map { it.toString() } ?: emptyList()
        val offlineAccess = config.getBoolean("offlineAccess")
        val hostedDomain = config.getString("hostedDomain")
        val forceCodeForRefreshToken = config.getBoolean("forceCodeForRefreshToken")
        val accountName = config.getString("accountName")

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
            gsoBuilder.requestScopes(Scope(scope))
        }

        hostedDomain?.let { domain ->
            gsoBuilder.setHostedDomain(domain)
        }

        val googleSignInOptions = gsoBuilder.build()
        googleSignInClient = GoogleSignIn.getClient(reactContext, googleSignInOptions)

        isConfigured = true
    }

    // MARK: - Sign In Methods

    override fun hasPlayServices(options: ReadableMap?, promise: Promise) {
        val activity = getCurrentActivity()
        if (activity == null) {
            promise.reject(NAME, "activity is null")
            return
        }

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val result = googleApiAvailability.isGooglePlayServicesAvailable(activity)

        if (result != ConnectionResult.SUCCESS) {
            val showPlayServicesUpdateDialog = options?.getBoolean("showPlayServicesUpdateDialog") ?: false
            if (showPlayServicesUpdateDialog && googleApiAvailability.isUserResolvableError(result)) {
                val requestCode = 2404
                googleApiAvailability.getErrorDialog(activity, result, requestCode).show()
            }
            promise.reject(PLAY_SERVICES_NOT_AVAILABLE, "Play services not available")
        } else {
            promise.resolve(true)
        }
    }

    override fun signIn(options: ReadableMap?, promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        val activity = getCurrentActivity()
        if (activity == null) {
            promise.reject(NAME, "activity is null")
            return
        }

        signInOrAddScopesPromiseWrapper.setPromiseWithInProgressCheck(promise, "signIn")
        UiThreadUtil.runOnUiThread {
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

        silentSignInPromiseWrapper.setPromiseWithInProgressCheck(promise, "signInSilently")
        UiThreadUtil.runOnUiThread {
            googleSignInClient?.let { client ->
                val result = client.silentSignIn()
                if (result.isSuccessful) {
                    handleSignInTaskResult(result, silentSignInPromiseWrapper)
                } else {
                    result.addOnCompleteListener { task ->
                        handleSignInTaskResult(task, silentSignInPromiseWrapper)
                    }
                }
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

        val activity = getCurrentActivity()
        if (activity == null) {
            promise.reject(NAME, "activity is null")
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account == null) {
            promise.resolve(false)
            return
        }

        signInOrAddScopesPromiseWrapper.setPromiseWithInProgressCheck(promise, "addScopes")

        val scopeList = scopes.toArrayList().map { Scope(it.toString()) }
        GoogleSignIn.requestPermissions(
            activity,
            REQUEST_CODE_ADD_SCOPES,
            account,
            *scopeList.toTypedArray()
        )
    }

    // MARK: - Sign Out Methods

    override fun signOut(promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        googleSignInClient?.let { client ->
            client.signOut()
                .addOnCompleteListener { task ->
                    handleSignOutOrRevokeAccessTask(task, promise)
                }
        } ?: run {
            promise.reject(NAME, "apiClient is null - call configure() first")
        }
    }

    override fun revokeAccess(promise: Promise) {
        if (!isConfigured) {
            promise.reject("not_configured", "Google Sign In is not configured. Call configure() first.")
            return
        }

        googleSignInClient?.let { client ->
            client.revokeAccess()
                .addOnCompleteListener { task ->
                    handleSignOutOrRevokeAccessTask(task, promise)
                }
        } ?: run {
            promise.reject(NAME, "apiClient is null - call configure() first")
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
        tokenClearingPromiseWrapper.setPromiseWithInProgressCheck(promise, "clearCachedAccessToken")
        executor.execute {
            try {
                GoogleAuthUtil.clearToken(reactContext, accessToken)
                tokenClearingPromiseWrapper.resolve(null)
            } catch (e: Exception) {
                tokenClearingPromiseWrapper.reject(e)
            }
        }
    }

    override fun getTokens(promise: Promise) {
        val account = GoogleSignIn.getLastSignedInAccount(reactContext)
        if (account == null) {
            promise.reject("getTokens", "getTokens requires a user to be signed in")
            return
        }

        tokenRetrievalPromiseWrapper.setPromiseWithInProgressCheck(promise, "getTokens")
        startTokenRetrievalTaskWithRecovery(account)
    }

    // MARK: - Activity Event Listener

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                handleSignInTaskResult(task, signInOrAddScopesPromiseWrapper)
            }
            REQUEST_CODE_ADD_SCOPES -> {
                if (resultCode == Activity.RESULT_OK) {
                    signInOrAddScopesPromiseWrapper.resolve(true)
                } else {
                    signInOrAddScopesPromiseWrapper.reject("Failed to add scopes.")
                }
            }
            REQUEST_CODE_RECOVER_AUTH -> {
                if (resultCode == Activity.RESULT_OK) {
                    rerunFailedAuthTokenTask()
                } else {
                    tokenRetrievalPromiseWrapper.reject("Failed authentication recovery attempt, probably user-rejected.")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // Not needed for Google Sign In
    }

    // MARK: - Helper Methods

    private fun handleSignInTaskResult(task: Task<GoogleSignInAccount>, promiseWrapper: PromiseWrapper) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account == null) {
                promiseWrapper.reject("GoogleSignInAccount instance was null")
            } else {
                val userParams = convertAccountToMap(account)
                promiseWrapper.resolve(userParams)
            }
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
                promiseWrapper.reject(
                    CommonStatusCodes.DEVELOPER_ERROR.toString(),
                    "DEVELOPER_ERROR: Follow troubleshooting instructions at https://react-native-google-signin.github.io/docs/troubleshooting"
                )
            } else {
                promiseWrapper.reject(e)
            }
        }
    }

    private fun handleSignOutOrRevokeAccessTask(task: Task<Void>, promise: Promise) {
        if (task.isSuccessful) {
            promise.resolve(null)
        } else {
            val exception = task.exception
            if (exception is ApiException) {
                val errorDescription = GoogleSignInStatusCodes.getStatusCodeString(exception.statusCode)
                promise.reject(exception.statusCode.toString(), errorDescription)
            } else {
                promise.reject("unknown_error", exception?.message ?: "Unknown error")
            }
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

    private fun startTokenRetrievalTaskWithRecovery(account: GoogleSignInAccount) {
        val userParams = convertAccountToMap(account)
        val recoveryParams = Arguments.createMap().apply {
            putBoolean(SHOULD_RECOVER, true)
        }
        AccessTokenRetrievalTask(this).execute(userParams, recoveryParams)
    }

    private fun rerunFailedAuthTokenTask() {
        val userProperties = pendingAuthRecovery?.getUserProperties()
        if (userProperties != null) {
            AccessTokenRetrievalTask(this).execute(userProperties, null)
        } else {
            tokenRetrievalPromiseWrapper.reject("rerunFailedAuthTokenTask: recovery failed")
        }
    }

    // MARK: - Inner Classes

    private class AccessTokenRetrievalTask(private val module: RnGoogleSigninModule) {
        fun execute(vararg params: WritableMap) {
            module.executor.execute {
                val userProperties = params[0]
                try {
                    insertAccessTokenIntoUserProperties(userProperties)
                    module.tokenRetrievalPromiseWrapper.resolve(userProperties)
                } catch (e: Exception) {
                    val recoverySettings = if (params.size >= 2) params[1] else null
                    handleException(e, userProperties, recoverySettings)
                }
            }
        }

        private fun insertAccessTokenIntoUserProperties(userProperties: WritableMap) {
            val mail = userProperties.getMap("user").getString("email")
            val scopes = userProperties.getArray("scopes")
            val scopeString = scopes.toArrayList().joinToString(" ")
            
            val token = GoogleAuthUtil.getToken(
                module.reactContext,
                android.accounts.Account(mail, "com.google"),
                scopeString
            )
            
            userProperties.putString("accessToken", token)
        }

        private fun handleException(cause: Exception, userProperties: WritableMap, settings: WritableMap?) {
            val isRecoverable = cause is UserRecoverableAuthException
            if (isRecoverable) {
                val shouldRecover = settings?.getBoolean(SHOULD_RECOVER) ?: false
                if (shouldRecover) {
                    attemptRecovery(cause, userProperties)
                } else {
                    module.tokenRetrievalPromiseWrapper.reject(cause)
                }
            } else {
                module.tokenRetrievalPromiseWrapper.reject(cause)
            }
        }

        private fun attemptRecovery(e: Exception, userProperties: WritableMap) {
            val activity = module.getCurrentActivity()
            if (activity == null) {
                module.pendingAuthRecovery = null
                module.tokenRetrievalPromiseWrapper.reject(
                    "Cannot attempt recovery auth because app is not in foreground. ${e.localizedMessage}"
                )
            } else {
                module.pendingAuthRecovery = PendingAuthRecovery(userProperties)
                val recoveryIntent = (e as UserRecoverableAuthException).intent
                activity.startActivityForResult(recoveryIntent, REQUEST_CODE_RECOVER_AUTH)
            }
        }
    }

    private class PendingAuthRecovery(private val userProperties: WritableMap) {
        fun getUserProperties(): WritableMap = userProperties
    }

    private class PromiseWrapper(private val moduleName: String) {
        private var promise: Promise? = null
        private var operationName: String? = null

        fun setPromiseWithInProgressCheck(promise: Promise, operationName: String) {
            if (this.promise != null) {
                promise.reject(moduleName, "$operationName: $ASYNC_OP_IN_PROGRESS")
                return
            }
            this.promise = promise
            this.operationName = operationName
        }

        fun resolve(result: Any?) {
            promise?.resolve(result)
            promise = null
            operationName = null
        }

        fun reject(error: String) {
            promise?.reject(moduleName, error)
            promise = null
            operationName = null
        }

        fun reject(throwable: Throwable) {
            promise?.reject(moduleName, throwable.message ?: "Unknown error", throwable)
            promise = null
            operationName = null
        }

        fun reject(code: String, message: String) {
            promise?.reject(code, message)
            promise = null
            operationName = null
        }
    }
}
