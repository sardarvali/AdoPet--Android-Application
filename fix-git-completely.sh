#!/bin/bash
# Aggressive removal of sensitive files from Git

echo "========================================"
echo "AGGRESSIVE Git Cleanup"
echo "========================================"
echo ""

echo "Step 1: Remove ALL staged files..."
git reset HEAD

echo ""
echo "Step 2: Force remove sensitive files from Git tracking..."
echo ""

# Force remove from Git index (not from disk)
git rm --cached --force google-services.json 2>/dev/null && echo "✅ Removed google-services.json from Git"
git rm --cached --force app/google-services.json 2>/dev/null && echo "✅ Removed app/google-services.json from Git"
git rm --cached --force local.properties 2>/dev/null && echo "✅ Removed local.properties from Git"
git rm --cached --force serviceAccountKey.json 2>/dev/null && echo "✅ Removed serviceAccountKey.json from Git"
git rm --cached --force gradle.properties 2>/dev/null && echo "✅ Removed gradle.properties from Git"

echo ""
echo "Step 3: Update .gitignore to be at the root..."
echo ""

# Make sure .gitignore is staged
git add .gitignore
echo "✅ Added .gitignore"

echo ""
echo "Step 4: Add everything EXCEPT sensitive files..."
echo ""

# Add everything else
git add .
echo "✅ Staged all files (sensitive files should be ignored)"

echo ""
echo "Step 5: Final verification..."
echo ""

# Check if sensitive files are still staged
if git diff --cached --name-only | grep -q "google-services.json"; then
    echo "❌ google-services.json is STILL staged - manual intervention needed"
    git status | grep google-services.json
else
    echo "✅ google-services.json is NOT staged"
fi

if git diff --cached --name-only | grep -q "local.properties"; then
    echo "❌ local.properties is STILL staged - manual intervention needed"
    git status | grep local.properties
else
    echo "✅ local.properties is NOT staged"
fi

echo ""
echo "========================================"
echo "Cleanup Complete!"
echo "========================================"
echo ""

echo "Files currently staged:"
git diff --cached --name-only | wc -l
echo ""

echo "Run: ./check-staged-files.sh to verify"
echo ""

