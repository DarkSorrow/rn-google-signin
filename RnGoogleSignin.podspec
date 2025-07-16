require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "RnGoogleSignin"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/DarkSorrow/rn-google-signin.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,cpp,swift}"
  s.private_header_files = "ios/**/*.h"
  
  # Swift bridging header and module map
  s.pod_target_xcconfig = {
    'SWIFT_OBJC_BRIDGING_HEADER' => '$(PODS_TARGET_SRCROOT)/ios/RnGoogleSignin-Bridging-Header.h',
    'DEFINES_MODULE' => 'YES',
    'SWIFT_INSTALL_OBJC_HEADER' => 'NO'
  }
  
  s.module_map = "ios/module.modulemap"

  # Use local Google Sign-In SDK from submodule
  s.dependency "GoogleSignIn", :path => "ios/google_signin"
  # Use remote GoogleSignInSwiftSupport since we can't modify the submodule
  s.dependency "GoogleSignInSwiftSupport", "~> 9.0"

  install_modules_dependencies(s)
end
