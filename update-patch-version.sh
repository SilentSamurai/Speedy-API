#!/bin/bash

# === CONFIGURATION ===
GROUP_ID="com.github.silentsamurai"
ARTIFACT_ID="speedy-api-parent"

# Convert groupId dots to slashes for Maven Central URL path
ENCODED_GROUP_ID=$(echo "$GROUP_ID" | sed 's/\./\//g')
ENCODED_ARTIFACT_ID=$ARTIFACT_ID

echo "Fetching latest version of $GROUP_ID:$ARTIFACT_ID from Maven Central..."

# === CHECK DEPENDENCIES ===
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven (mvn) is required but not installed or not in PATH."
    exit 1
fi

# === FETCH LATEST VERSION FROM MAVEN CENTRAL ===
METADATA_URL="https://repo1.maven.org/maven2/$ENCODED_GROUP_ID/$ENCODED_ARTIFACT_ID/maven-metadata.xml"
echo "🔍 Fetching from URL: $METADATA_URL"

# Fetch the metadata XML and extract the latest version
LATEST_VERSION=$(curl -s "$METADATA_URL" | grep -o '<latest>[^<]*</latest>' | sed 's/<latest>\(.*\)<\/latest>/\1/')

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
