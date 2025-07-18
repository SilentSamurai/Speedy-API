#!/bin/bash

# === CONFIGURATION ===
GROUP_ID="com.github.silentsamurai"       # Replace with your groupId
ARTIFACT_ID="speedy-api-parent"  # Replace with your artifactId

# URL encode the groupId and artifactId
ENCODED_GROUP_ID=$(echo "$GROUP_ID" | sed 's/\./%2E/g')
ENCODED_ARTIFACT_ID=$ARTIFACT_ID

echo "Fetching latest version of $GROUP_ID:$ARTIFACT_ID from Maven Central..."

# === CHECK DEPENDENCIES ===
if ! command -v jq &> /dev/null; then
    echo "❌ Error: jq is required but not installed. Please install jq first."
    echo "   On Ubuntu/Debian: sudo apt-get install jq"
    echo "   On macOS: brew install jq"
    echo "   On Windows: choco install jq"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven (mvn) is required but not installed or not in PATH."
    exit 1
fi

# === FETCH LATEST VERSION FROM MAVEN CENTRAL ===
SEARCH_URL="https://search.maven.org/solrsearch/select?q=g:%22$ENCODED_GROUP_ID%22+AND+a:%22$ENCODED_ARTIFACT_ID%22&rows=1&wt=json"
echo $SEARCH_URL
LATEST_VERSION=$(curl -s "$SEARCH_URL" \
  | jq -r '.response.docs[0].latestVersion')

if [[ -z "$LATEST_VERSION" || "$LATEST_VERSION" == "null" ]]; then
  echo "⚠️  Could not fetch latest version from Maven Central."
  echo "   This might be the first release or the artifact doesn't exist yet."
  echo "   Using current local version as base..."
  
  # Get current local version
  CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Could not read current project version from pom.xml"
    exit 1
  fi
  
  echo "Current local version: $CURRENT_VERSION"
  echo "Using this as the base version for incrementing..."
  
  # Use current version as the base
  LATEST_VERSION=$CURRENT_VERSION
fi

echo "Latest version found: $LATEST_VERSION"

# === GET CURRENT LOCAL VERSION ===
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
if [[ $? -ne 0 ]]; then
    echo "❌ Error: Could not read current project version from pom.xml"
    exit 1
fi

echo "Current local version: $CURRENT_VERSION"

# === PARSE AND INCREMENT PATCH VERSION ===
IFS='.' read -r MAJOR MINOR PATCH <<< "$LATEST_VERSION"
if [[ -z "$MAJOR" || -z "$MINOR" || -z "$PATCH" ]]; then
  echo "❌ Invalid version format: $LATEST_VERSION"
  exit 1
fi

NEW_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"
echo "Proposed new patch version: $NEW_VERSION"

# === SET NEW VERSION LOCALLY ===
echo "Updating all modules to version $NEW_VERSION ..."
if mvn versions:set -DnewVersion="$NEW_VERSION" -DprocessAllModules=true -DgenerateBackupPoms=false; then
    echo "✅ Successfully updated all modules to $NEW_VERSION"
else
    echo "❌ Failed to update versions. Please check the Maven output above."
    exit 1
fi
