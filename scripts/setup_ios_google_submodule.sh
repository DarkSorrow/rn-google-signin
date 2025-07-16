# PowerShell: Add or update GoogleSignIn submodule in ios_google
$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Split-Path -Parent $root
$deps = Get-Content "$projectRoot\\dependencies.json" | ConvertFrom-Json
$url = $deps.dependencies.googleSignIn.ios.repository
$commit = $deps.dependencies.googleSignIn.ios.commit
$submodule = Join-Path $projectRoot 'ios_google'

Write-Host "Repository: $url"
Write-Host "Commit: $commit"
Write-Host "Target: $submodule"

if (Test-Path $submodule) {
    Write-Host "Removing existing submodule at $submodule"
    git submodule deinit -f $submodule 2>$null
    git rm -f $submodule 2>$null
    Remove-Item -Recurse -Force $submodule -ErrorAction SilentlyContinue
}
git submodule add https://github.com/google/GoogleSignIn-iOS ios_google && cd ios_google && git checkout 3996d908c7b3ce8a87d39c808f9a6b2a08fbe043 && cd ..
if ($LASTEXITCODE -ne 0) { throw "Failed to add submodule" }
Push-Location $submodule
git checkout $commit
if ($LASTEXITCODE -ne 0) { throw "Failed to checkout commit $commit" }
Pop-Location

if (Test-Path (Join-Path $submodule 'GoogleSignIn.podspec')) {
    Write-Host "✅ Submodule at ios_google checked out to $commit"
    Write-Host "Update your podspec: s.dependency 'GoogleSignIn', :path => 'ios_google'"
} else {
    Write-Host "❌ Submodule setup failed. Podspec not found."
    exit 1
} 