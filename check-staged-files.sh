#!/bin/bash
# Quick verification script for Git staged files

echo "========================================"
echo "Checking Staged Files for Sensitive Data"
echo "========================================"
echo ""

# Get list of staged files
staged_files=$(git diff --cached --name-only)

# Flag to track if any issues found
issues_found=0

echo "Checking for sensitive files..."
echo ""

# Check for google-services.json
if echo "$staged_files" | grep -q "google-services.json"; then
    echo "❌ CRITICAL: google-services.json is staged!"
    echo "   Run: git reset HEAD google-services.json"
    issues_found=1
else
    echo "✅ google-services.json - Not staged (Good)"
fi

# Check for serviceAccountKey.json
if echo "$staged_files" | grep -q "serviceAccountKey.json"; then
    echo "❌ CRITICAL: serviceAccountKey.json is staged!"
    echo "   Run: git reset HEAD serviceAccountKey.json"
    issues_found=1
else
    echo "✅ serviceAccountKey.json - Not staged (Good)"
fi

# Check for local.properties
if echo "$staged_files" | grep -q "local.properties"; then
    echo "❌ CRITICAL: local.properties is staged!"
    echo "   Run: git reset HEAD local.properties"
    issues_found=1
else
    echo "✅ local.properties - Not staged (Good)"
fi

# Check for keystore files
if echo "$staged_files" | grep -qE "\.(jks|keystore)$"; then
    echo "❌ CRITICAL: Keystore file is staged!"
    echo "   Run: git reset HEAD *.jks *.keystore"
    issues_found=1
else
    echo "✅ Keystore files - Not staged (Good)"
fi

# Check for gradle.properties (might contain keys)
if echo "$staged_files" | grep -q "^gradle.properties$"; then
    echo "⚠️  WARNING: gradle.properties is staged"
    echo "   Make sure it doesn't contain real API keys!"
else
    echo "✅ gradle.properties - Not staged (Good)"
fi

echo ""
echo "========================================"

if [ $issues_found -eq 0 ]; then
    echo "✅ ALL CHECKS PASSED!"
    echo "Safe to commit."
    echo ""
    echo "Next command:"
    echo 'git commit -m "Initial commit: Pet Adoption Android App"'
else
    echo "❌ ISSUES FOUND - DO NOT COMMIT YET!"
    echo "Remove sensitive files first."
fi

echo "========================================"
echo ""
echo "Total files staged: $(echo "$staged_files" | wc -l)"
echo ""

