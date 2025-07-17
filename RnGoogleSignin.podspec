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

  s.source_files = "ios/**/*.{h,m,mm,cpp}"
  s.private_header_files = "ios/**/*.h"
  
  # Build as static library (default for React Native modules)
  s.static_framework = true

  # Use official Google Sign-In SDK from remote repository
  s.dependency "GoogleSignIn", "~> 9.0"
  s.dependency "GoogleSignInSwiftSupport", "~> 9.0"

  install_modules_dependencies(s)
end
