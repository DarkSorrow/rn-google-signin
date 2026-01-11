package com.novastera.rngooglesignin

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class RnGoogleSigninPackage : TurboReactPackage() {

  override fun getModule(
    name: String,
    reactContext: ReactApplicationContext
  ): NativeModule? {
    return if (name == RnGoogleSigninModule.NAME) {
      RnGoogleSigninModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      mapOf(
        RnGoogleSigninModule.NAME to ReactModuleInfo(
          RnGoogleSigninModule.NAME,
          RnGoogleSigninModule.NAME,
          false, // canOverrideExistingModule
          false, // needsEagerInit
          true,  // isTurboModule
          false  // hasConstants
        )
      )
    }
  }
}
