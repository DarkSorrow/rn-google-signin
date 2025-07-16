# PowerShell script for Windows users

# Get script directory and project root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Split-Path -Parent $ScriptDir

# Configuration files
$DependenciesFile = Join-Path $ProjectRoot "dependencies.json"

# Check if configuration files exist
if (-not (Test-Path $DependenciesFile)) {
    Write-Host "❌ Error: dependencies.json not found at $DependenciesFile" -ForegroundColor Red
    exit 1
}

# Read and parse JSON files
try {
    $DependenciesConfig = Get-Content $DependenciesFile -Raw | ConvertFrom-Json
    
    # Extract values
    $PACKAGE_NAME = "GoogleSignIn"
    $GOOGLE_SIGNIN_VERSION = $DependenciesConfig.dependencies.googleSignIn.ios.version
    $PACKAGE_COMMIT = $DependenciesConfig.dependencies.googleSignIn.ios.commit
    $PACKAGE_URL = $DependenciesConfig.dependencies.googleSignIn.ios.repository
    $SUBMODULE_PATH = Join-Path $ProjectRoot "ios_google"
    
    # Validate extracted values
    if (-not $GOOGLE_SIGNIN_VERSION -or -not $PACKAGE_COMMIT -or -not $PACKAGE_URL) {
        Write-Host "❌ Error: Could not extract required configuration values" -ForegroundColor Red
        Write-Host "GoogleSignIn version: $GOOGLE_SIGNIN_VERSION" -ForegroundColor Gray
        Write-Host "Commit: $PACKAGE_COMMIT" -ForegroundColor Gray
        Write-Host "URL: $PACKAGE_URL" -ForegroundColor Gray
        exit 1
    }
    
} catch {
    Write-Host "❌ Error: Failed to parse dependencies.json" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Gray
    exit 1
}

Write-Host "📋 Configuration:" -ForegroundColor Green
Write-Host "Package: $PACKAGE_NAME" -ForegroundColor White
Write-Host "Version: $GOOGLE_SIGNIN_VERSION" -ForegroundColor White
Write-Host "Commit: $PACKAGE_COMMIT" -ForegroundColor White
Write-Host "Repository: $PACKAGE_URL" -ForegroundColor White

Write-Host ""
Write-Host "🔧 Setting up iOS Google Sign-In submodule..." -ForegroundColor Green

# Check if git is available
try {
    $gitVersion = git --version
    Write-Host "✅ Git found: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: git is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if we're in a git repository
if (-not (Test-Path ".git")) {
    Write-Host "❌ Error: Not in a git repository. Please run this from the project root." -ForegroundColor Red
    exit 1
}

# Remove existing submodule if it exists
if (Test-Path $SUBMODULE_PATH) {
    Write-Host "🗑️ Removing existing submodule at $SUBMODULE_PATH" -ForegroundColor Yellow
    
    try {
        # Deinitialize submodule
        git submodule deinit -f $SUBMODULE_PATH 2>$null
        # Remove from git
        git rm -f $SUBMODULE_PATH 2>$null
        # Remove directory
        Remove-Item -Recurse -Force $SUBMODULE_PATH -ErrorAction SilentlyContinue
        # Remove from .git/modules
        $gitModulesPath = Join-Path ".git" "modules" (Split-Path $SUBMODULE_PATH -Leaf)
        if (Test-Path $gitModulesPath) {
            Remove-Item -Recurse -Force $gitModulesPath -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Host "⚠️ Warning: Could not clean up existing submodule completely" -ForegroundColor Yellow
    }
}

# Add the submodule
Write-Host "➕ Adding submodule..." -ForegroundColor Green
try {
    git submodule add $PACKAGE_URL $SUBMODULE_PATH
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to add submodule"
    }
} catch {
    Write-Host "❌ Error: Failed to add submodule" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Gray
    exit 1
}

# Checkout the specific commit
Write-Host "🔍 Checking out commit: $PACKAGE_COMMIT" -ForegroundColor Green
try {
    Push-Location $SUBMODULE_PATH
    git checkout $PACKAGE_COMMIT
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Error: Failed to checkout commit $PACKAGE_COMMIT" -ForegroundColor Red
        Write-Host "Available commits:" -ForegroundColor Yellow
        git log --oneline -10
        Pop-Location
        exit 1
    }
    Pop-Location
} catch {
    Write-Host "❌ Error: Failed to checkout commit" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Gray
    Pop-Location
    exit 1
}

# Verify the submodule is set up correctly
$PodspecPath = Join-Path $SUBMODULE_PATH "GoogleSignIn.podspec"
if ((Test-Path $SUBMODULE_PATH) -and (Test-Path $PodspecPath)) {
    Write-Host "✅ Submodule setup completed successfully!" -ForegroundColor Green
    Write-Host "📁 Submodule location: $SUBMODULE_PATH" -ForegroundColor White
    Write-Host "📄 Podspec found: $PodspecPath" -ForegroundColor White
    
    # Show the current commit
    try {
        Push-Location $SUBMODULE_PATH
        $CURRENT_COMMIT = git rev-parse HEAD
        Pop-Location
        
        Write-Host "🔗 Current commit: $CURRENT_COMMIT" -ForegroundColor White
        
        if ($CURRENT_COMMIT -eq $PACKAGE_COMMIT) {
            Write-Host "✅ Commit matches expected: $PACKAGE_COMMIT" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Warning: Commit mismatch. Expected: $PACKAGE_COMMIT, Got: $CURRENT_COMMIT" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠️ Warning: Could not verify commit" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Error: Submodule setup failed" -ForegroundColor Red
    Write-Host "Expected files not found in $SUBMODULE_PATH" -ForegroundColor Gray
    exit 1
}

Write-Host ""
Write-Host "🎯 Next steps:" -ForegroundColor Green
Write-Host "1. Update your podspec to use the local submodule:" -ForegroundColor White
Write-Host "   s.dependency 'GoogleSignIn', :path => 'ios_google'" -ForegroundColor Cyan
Write-Host "2. Run 'pod install' in your iOS project" -ForegroundColor White
Write-Host "3. Clean and rebuild your project" -ForegroundColor White 