#!/bin/bash
# Script to remove sensitive files from Git tracking

echo "========================================"
echo "Removing Sensitive Files from Git"
echo "========================================"
echo ""

echo "Step 1: Unstaging sensitive files..."
echo ""

# Unstage google-services.json
if git diff --cached --name-only | grep -q "google-services.json"; then
    git reset HEAD google-services.json 2>/dev/null
    git reset HEAD app/google-services.json 2>/dev/null
    git reset HEAD **/google-services.json 2>/dev/null
    echo "✅ Unstaged google-services.json"
else
    echo "✅ google-services.json was not staged"
fi

# Unstage local.properties
if git diff --cached --name-only | grep -q "local.properties"; then
    git reset HEAD local.properties 2>/dev/null
    git reset HEAD **/local.properties 2>/dev/null
    echo "✅ Unstaged local.properties"
else
    echo "✅ local.properties was not staged"
fi

# Unstage serviceAccountKey.json
if git diff --cached --name-only | grep -q "serviceAccountKey.json"; then
    git reset HEAD serviceAccountKey.json 2>/dev/null
    echo "✅ Unstaged serviceAccountKey.json"
else
    echo "✅ serviceAccountKey.json was not staged"
fi

# Unstage gradle.properties if it exists
if git diff --cached --name-only | grep -q "gradle.properties"; then
    git reset HEAD gradle.properties 2>/dev/null
    echo "✅ Unstaged gradle.properties"
fi

echo ""
echo "Step 2: Removing from Git cache (if previously tracked)..."
echo ""

# Remove from cache if they were previously tracked
git rm --cached google-services.json 2>/dev/null && echo "✅ Removed google-services.json from cache"
git rm --cached app/google-services.json 2>/dev/null && echo "✅ Removed app/google-services.json from cache"
git rm --cached local.properties 2>/dev/null && echo "✅ Removed local.properties from cache"
git rm --cached serviceAccountKey.json 2>/dev/null && echo "✅ Removed serviceAccountKey.json from cache"
git rm --cached gradle.properties 2>/dev/null

echo ""
echo "Step 3: Verifying .gitignore..."
echo ""

if grep -q "google-services.json" .gitignore; then
    echo "✅ google-services.json is in .gitignore"
else
    echo "⚠️  Adding google-services.json to .gitignore"
    echo "google-services.json" >> .gitignore
fi

if grep -q "local.properties" .gitignore; then
    echo "✅ local.properties is in .gitignore"
else
    echo "⚠️  Adding local.properties to .gitignore"
    echo "local.properties" >> .gitignore
fi

echo ""
echo "========================================"
echo "Cleanup Complete!"
echo "========================================"
echo ""

echo "Now run: ./check-staged-files.sh"
echo "to verify everything is clean."
echo ""

