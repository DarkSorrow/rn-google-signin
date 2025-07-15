# PowerShell script for Windows users

# Get script directory and project root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Split-Path -Parent $ScriptDir

# Configuration files
$DependenciesFile = Join-Path $ProjectRoot "dependencies.json"
$PackageFile = Join-Path $ProjectRoot "package.json"

# Check if configuration files exist
if (-not (Test-Path $DependenciesFile)) {
    Write-Host "❌ Error: dependencies.json not found at $DependenciesFile" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $PackageFile)) {
    Write-Host "❌ Error: package.json not found at $PackageFile" -ForegroundColor Red
    exit 1
}

# Read and parse JSON files
try {
    $DependenciesConfig = Get-Content $DependenciesFile -Raw | ConvertFrom-Json
    $PackageConfig = Get-Content $PackageFile -Raw | ConvertFrom-Json
    
    # Extract values
    $PACKAGE_NAME = "GoogleSignIn"
    $PACKAGE_VERSION = $PackageConfig.version
    $GOOGLE_SIGNIN_VERSION = $DependenciesConfig.dependencies.googleSignIn.ios.version
    $PACKAGE_COMMIT = $DependenciesConfig.dependencies.googleSignIn.ios.commit
    $PACKAGE_URL = $DependenciesConfig.dependencies.googleSignIn.ios.repository
    $SUBMODULE_PATH = "ios/third-party/GoogleSignIn-iOS"
    
    # Validate extracted values
    if (-not $PACKAGE_VERSION -or -not $GOOGLE_SIGNIN_VERSION -or -not $PACKAGE_COMMIT -or -not $PACKAGE_URL) {
        Write-Host "❌ Error: Could not extract required configuration values" -ForegroundColor Red
        Write-Host "Package version: $PACKAGE_VERSION" -ForegroundColor Gray
        Write-Host "GoogleSignIn version: $GOOGLE_SIGNIN_VERSION" -ForegroundColor Gray
        Write-Host "Commit: $PACKAGE_COMMIT" -ForegroundColor Gray
        Write-Host "URL: $PACKAGE_URL" -ForegroundColor Gray
        exit 1
    }
    
} catch {
    Write-Host "❌ Error: Failed to parse configuration files" -ForegroundColor Red
    Write-Host "Error details: $($_.Exception.Message)" -ForegroundColor Gray
    exit 1
}

Write-Host "🔧 Setting up iOS dependencies for $PACKAGE_NAME v$GOOGLE_SIGNIN_VERSION..." -ForegroundColor Cyan
Write-Host "📦 Package version: v$PACKAGE_VERSION" -ForegroundColor Cyan

# Check if we're in a git repository
try {
    git rev-parse --git-dir | Out-Null
} catch {
    Write-Host "❌ Error: Not in a git repository. Please run 'git init' first." -ForegroundColor Red
    exit 1
}

# Check if submodule already exists
if (Test-Path $SUBMODULE_PATH) {
    Write-Host "📁 Submodule already exists at $SUBMODULE_PATH" -ForegroundColor Yellow
    Write-Host "🔄 Updating submodule..." -ForegroundColor Cyan
    
    git submodule update --init --recursive $SUBMODULE_PATH
    
    # Navigate to submodule and checkout specific commit
    Write-Host "📌 Checking out commit: $PACKAGE_COMMIT" -ForegroundColor Cyan
    
    Push-Location $SUBMODULE_PATH
    try {
        git checkout $PACKAGE_COMMIT
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Successfully checked out commit $PACKAGE_COMMIT" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Warning: Could not checkout commit $PACKAGE_COMMIT" -ForegroundColor Yellow
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "➕ Adding $PACKAGE_NAME as Git submodule..." -ForegroundColor Cyan
    
    # Create directory if it doesn't exist
    $parentDir = Split-Path $SUBMODULE_PATH -Parent
    if (-not (Test-Path $parentDir)) {
        New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
    }
    
    # Add the submodule
    git submodule add $PACKAGE_URL $SUBMODULE_PATH
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Failed to add submodule" -ForegroundColor Red
        exit 1
    }
    
    # Initialize and update the submodule
    git submodule update --init --recursive $SUBMODULE_PATH
    
    # Checkout the specific commit
    Write-Host "📌 Checking out commit: $PACKAGE_COMMIT" -ForegroundColor Cyan
    
    Push-Location $SUBMODULE_PATH
    try {
        git checkout $PACKAGE_COMMIT
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Successfully checked out commit $PACKAGE_COMMIT" -ForegroundColor Green
            Pop-Location
            
            # Stage the changes
            git add .gitmodules $SUBMODULE_PATH
            Write-Host "📝 Changes staged. Run 'git commit' to save the submodule setup." -ForegroundColor Yellow
        } else {
            Write-Host "⚠️  Warning: Could not checkout commit $PACKAGE_COMMIT" -ForegroundColor Yellow
            Pop-Location
        }
    } catch {
        Pop-Location
        throw
    }
}

Write-Host ""
Write-Host "✅ $PACKAGE_NAME v$GOOGLE_SIGNIN_VERSION (commit: $PACKAGE_COMMIT) is ready!" -ForegroundColor Green
Write-Host "📁 Location: $SUBMODULE_PATH" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 Useful commands:" -ForegroundColor Cyan
Write-Host "   # Update submodule to latest" -ForegroundColor Gray
Write-Host "   git submodule update --remote $SUBMODULE_PATH" -ForegroundColor White
Write-Host ""
Write-Host "   # Update to a specific commit" -ForegroundColor Gray
Write-Host "   cd $SUBMODULE_PATH; git checkout <commit-hash>; cd -" -ForegroundColor White
Write-Host "   git add $SUBMODULE_PATH; git commit -m 'Update GoogleSignIn'" -ForegroundColor White
Write-Host ""
Write-Host "   # Initialize submodules when cloning this repo" -ForegroundColor Gray
Write-Host "   git submodule update --init --recursive" -ForegroundColor White 