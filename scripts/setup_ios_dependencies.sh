#!/bin/bash

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Function to extract JSON values (simple parser for our use case)
get_json_value() {
    local file="$1"
    local key="$2"
    grep -o "\"$key\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" "$file" | sed 's/.*"\([^"]*\)".*/\1/'
}

get_nested_json_value() {
    local file="$1"
    local path="$2"
    # For nested paths like "dependencies.googleSignIn.ios.version"
    python3 -c "import json, sys; data=json.load(open('$file')); print(data$(echo "$path" | sed 's/\./[\"/' | sed 's/\./\"]\[\"/' | sed 's/$/\"]/'))" 2>/dev/null || echo ""
}

# Read configuration
DEPENDENCIES_FILE="$PROJECT_ROOT/dependencies.json"
PACKAGE_FILE="$PROJECT_ROOT/package.json"

if [ ! -f "$DEPENDENCIES_FILE" ]; then
    echo "❌ Error: dependencies.json not found at $DEPENDENCIES_FILE"
    exit 1
fi

if [ ! -f "$PACKAGE_FILE" ]; then
    echo "❌ Error: package.json not found at $PACKAGE_FILE"
    exit 1
fi

# Extract values from JSON files
PACKAGE_VERSION=$(get_json_value "$PACKAGE_FILE" "version")
PACKAGE_NAME="GoogleSignIn"

# Try to read from dependencies.json using Python if available, otherwise fallback
if command -v python3 &> /dev/null; then
    GOOGLE_SIGNIN_VERSION=$(python3 -c "import json; data=json.load(open('$DEPENDENCIES_FILE')); print(data['dependencies']['googleSignIn']['ios']['version'])" 2>/dev/null)
    PACKAGE_COMMIT=$(python3 -c "import json; data=json.load(open('$DEPENDENCIES_FILE')); print(data['dependencies']['googleSignIn']['ios']['commit'])" 2>/dev/null)
    PACKAGE_URL=$(python3 -c "import json; data=json.load(open('$DEPENDENCIES_FILE')); print(data['dependencies']['googleSignIn']['ios']['repository'])" 2>/dev/null)
else
    # Fallback to manual parsing (less reliable but works without Python)
    GOOGLE_SIGNIN_VERSION=$(grep -A 10 '"ios"' "$DEPENDENCIES_FILE" | grep '"version"' | sed 's/.*"\([^"]*\)".*/\1/')
    PACKAGE_COMMIT=$(grep -A 10 '"ios"' "$DEPENDENCIES_FILE" | grep '"commit"' | sed 's/.*"\([^"]*\)".*/\1/')
    PACKAGE_URL=$(grep -A 10 '"ios"' "$DEPENDENCIES_FILE" | grep '"repository"' | sed 's/.*"\([^"]*\)".*/\1/')
fi

# Validate extracted values
if [ -z "$PACKAGE_VERSION" ] || [ -z "$GOOGLE_SIGNIN_VERSION" ] || [ -z "$PACKAGE_COMMIT" ] || [ -z "$PACKAGE_URL" ]; then
    echo "❌ Error: Could not extract required configuration values"
    echo "Package version: $PACKAGE_VERSION"
    echo "GoogleSignIn version: $GOOGLE_SIGNIN_VERSION"
    echo "Commit: $PACKAGE_COMMIT"
    echo "URL: $PACKAGE_URL"
    exit 1
fi

SUBMODULE_PATH="ios/third-party/GoogleSignIn-iOS"

echo "🔧 Setting up iOS dependencies for ${PACKAGE_NAME} v${GOOGLE_SIGNIN_VERSION}..."
echo "📦 Package version: v${PACKAGE_VERSION}"

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository. Please run 'git init' first."
    exit 1
fi

# Check if submodule already exists
if [ -d "$SUBMODULE_PATH" ]; then
    echo "📁 Submodule already exists at $SUBMODULE_PATH"
    echo "🔄 Updating submodule..."
    git submodule update --init --recursive "$SUBMODULE_PATH"
    
    # Navigate to submodule and checkout specific commit
    echo "📌 Checking out commit: $PACKAGE_COMMIT"
    (cd "$SUBMODULE_PATH" && git checkout "$PACKAGE_COMMIT")
    
    if [ $? -eq 0 ]; then
        echo "✅ Successfully checked out commit $PACKAGE_COMMIT"
    else
        echo "⚠️  Warning: Could not checkout commit $PACKAGE_COMMIT"
    fi
else
    echo "➕ Adding ${PACKAGE_NAME} as Git submodule..."
    
    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$SUBMODULE_PATH")"
    
    # Add the submodule
    git submodule add "$PACKAGE_URL" "$SUBMODULE_PATH"
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to add submodule"
        exit 1
    fi
    
    # Initialize and update the submodule
    git submodule update --init --recursive "$SUBMODULE_PATH"
    
    # Checkout the specific commit
    echo "📌 Checking out commit: $PACKAGE_COMMIT"
    (cd "$SUBMODULE_PATH" && git checkout "$PACKAGE_COMMIT")
    
    if [ $? -eq 0 ]; then
        echo "✅ Successfully checked out commit $PACKAGE_COMMIT"
        
        # Stage the changes
        git add .gitmodules "$SUBMODULE_PATH"
        echo "📝 Changes staged. Run 'git commit' to save the submodule setup."
    else
        echo "⚠️  Warning: Could not checkout commit $PACKAGE_COMMIT"
    fi
fi

echo ""
echo "✅ ${PACKAGE_NAME} v${GOOGLE_SIGNIN_VERSION} (commit: ${PACKAGE_COMMIT}) is ready!"
echo "📁 Location: ${SUBMODULE_PATH}"
echo ""
echo "💡 Useful commands:"
echo "   # Update submodule to latest"
echo "   git submodule update --remote $SUBMODULE_PATH"
echo ""
echo "   # Update to a specific commit"
echo "   cd $SUBMODULE_PATH && git checkout <commit-hash> && cd -"
echo "   git add $SUBMODULE_PATH && git commit -m \"Update GoogleSignIn\""
echo ""
echo "   # Initialize submodules when cloning this repo"
echo "   git submodule update --init --recursive" 