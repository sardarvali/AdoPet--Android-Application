#!/bin/bash
# Final setup - add files properly with .gitignore working

echo "=========================================="
echo "FINAL SETUP - Adding Files Correctly"
echo "=========================================="
echo ""

echo "Current situation: All files are untracked (shown as new)"
echo "This is GOOD - now .gitignore will work properly!"
echo ""

echo "Step 1: Add .gitignore first"
git add .gitignore .gitattributes
echo "✅ Added .gitignore and .gitattributes"
echo ""

echo "Step 2: Add all other files"
echo "(Sensitive files will be automatically ignored)"
git add .
echo "✅ Added all files"
echo ""

echo "Step 3: Verify sensitive files are NOT staged"
echo ""

ISSUES=0

if git diff --cached --name-only | grep -q "google-services.json"; then
    echo "❌ google-services.json is staged"
    ISSUES=1
else
    echo "✅ google-services.json is NOT staged"
fi

if git diff --cached --name-only | grep -q "local.properties"; then
    echo "❌ local.properties is staged"
    ISSUES=1
else
    echo "✅ local.properties is NOT staged"
fi

if git diff --cached --name-only | grep -q "serviceAccountKey.json"; then
    echo "❌ serviceAccountKey.json is staged"
    ISSUES=1
else
    echo "✅ serviceAccountKey.json is NOT staged"
fi

echo ""
echo "=========================================="

if [ $ISSUES -eq 0 ]; then
    echo "✅ ALL CHECKS PASSED!"
    echo ""
    echo "Total files staged: $(git diff --cached --name-only | wc -l)"
    echo ""
    echo "Next commands:"
    echo ""
    echo "1. git commit -m \"Initial commit: Pet Adoption Android App\""
    echo ""
    echo "2. Create GitHub repo at: https://github.com/new"
    echo ""
    echo "3. git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git"
    echo ""
    echo "4. git branch -M main"
    echo ""
    echo "5. git push -u origin main"
    echo ""
else
    echo "❌ ISSUES FOUND"
    echo ""
    echo "Run: ./check-staged-files.sh for details"
fi

echo "=========================================="

