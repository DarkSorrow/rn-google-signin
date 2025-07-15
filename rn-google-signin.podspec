require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
dependencies = JSON.parse(File.read(File.join(__dir__, "dependencies.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|
  s.name         = "rn-google-signin"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "12.0" }
  s.source       = { :git => "https://github.com/novastera/rn-google-signin.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.requires_arc = true

  s.dependency "React-Core"
  # Use GoogleSignIn from submodule when available, fallback to CocoaPods
  google_signin_version = dependencies["dependencies"]["googleSignIn"]["ios"]["version"]
  
  if File.exist?("ios/third-party/GoogleSignIn-iOS/GoogleSignIn.podspec")
    s.dependency "GoogleSignIn", :path => "ios/third-party/GoogleSignIn-iOS"
  else
    s.dependency "GoogleSignIn", "~> #{google_signin_version}"
  end
  
  # Turbo Module dependencies
  s.dependency "React-Core"
  s.dependency "React-RCTFabric"
  s.dependency "React-Codegen"
  s.dependency "RCT-Folly"
  s.dependency "RCTRequired"
  s.dependency "RCTTypeSafety"
  s.dependency "ReactCommon/turbomodule/core"

  # Enable C++ language support
  s.pod_target_xcconfig = {
    "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
    "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\""
  }
  
  s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
  
  # Turbo Module configuration
  s.install_modules_dependencies = true
end 