package com.novastera.rngooglesignin

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.turbomodule.core.CallInvokerHolderImpl

abstract class NativeGoogleSigninSpec(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    @ReactMethod
    abstract fun configure(config: ReadableMap, promise: Promise)

    @ReactMethod
    abstract fun hasPlayServices(promise: Promise)

    @ReactMethod
    abstract fun signIn(promise: Promise)

    @ReactMethod
    abstract fun signInSilently(promise: Promise)

    @ReactMethod
    abstract fun addScopes(scopes: ReadableArray, promise: Promise)

    @ReactMethod
    abstract fun signOut(promise: Promise)

    @ReactMethod
    abstract fun revokeAccess(promise: Promise)

    @ReactMethod
    abstract fun isSignedIn(promise: Promise)

    @ReactMethod
    abstract fun getCurrentUser(promise: Promise)

    @ReactMethod
    abstract fun clearCachedAccessToken(accessToken: String, promise: Promise)

    @ReactMethod
    abstract fun getTokens(promise: Promise)
} 