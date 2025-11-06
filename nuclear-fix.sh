#!/bin/bash
# Nuclear option - completely remove and re-add with proper .gitignore

echo "=========================================="
echo "NUCLEAR OPTION - Complete Reset"
echo "=========================================="
echo ""

echo "Step 1: Completely unstage everything"
git reset HEAD
echo "✅ All files unstaged"
echo ""

echo "Step 2: Find and display sensitive files locations"
echo ""
echo "Files that exist on disk:"
find . -name "google-services.json" -not -path "*/node_modules/*" -not -path "*/.git/*" 2>/dev/null
find . -name "local.properties" -not -path "*/node_modules/*" -not -path "*/.git/*" 2>/dev/null
echo ""

echo "Step 3: Manually exclude each sensitive file"
echo ""

# Create exclusion list
cat > .git/info/exclude << 'EOF'
# Sensitive files - DO NOT COMMIT
google-services.json
app/google-services.json
local.properties
serviceAccountKey.json
gradle.properties
*.jks
*.keystore
EOF

echo "✅ Created Git exclusion list"
echo ""

echo "Step 4: Add .gitignore patterns at the beginning"
echo ""

# Make sure .gitignore has proper patterns at the top
if ! head -n 20 .gitignore | grep -q "^google-services.json$"; then
    echo "Adding patterns to .gitignore..."
    # Create temp file with sensitive files at top
    cat > .gitignore.tmp << 'EOF'
# CRITICAL - DO NOT COMMIT THESE FILES
google-services.json
app/google-services.json
**/google-services.json
local.properties
**/local.properties
serviceAccountKey.json
gradle.properties
*.jks
*.keystore

EOF
    cat .gitignore >> .gitignore.tmp
    mv .gitignore.tmp .gitignore
    echo "✅ Updated .gitignore"
fi
echo ""

echo "Step 5: Add files with explicit exclusions"
echo ""

# Add .gitignore first
git add .gitignore .gitattributes
echo "✅ Added .gitignore"

# Add files but explicitly exclude sensitive ones
git add -- ':!google-services.json' \
         ':!app/google-services.json' \
         ':!local.properties' \
         ':!serviceAccountKey.json' \
         ':!gradle.properties' \
         ':!*.jks' \
         ':!*.keystore' \
         .

echo "✅ Added files with exclusions"
echo ""

echo "Step 6: Final verification"
echo ""

ISSUES=0

if git diff --cached --name-only | grep -q "google-services.json"; then
    echo "❌ google-services.json STILL staged"
    ISSUES=1
else
    echo "✅ google-services.json NOT staged"
fi

if git diff --cached --name-only | grep -q "local.properties"; then
    echo "❌ local.properties STILL staged"
    ISSUES=1
else
    echo "✅ local.properties NOT staged"
fi

if git diff --cached --name-only | grep -q "serviceAccountKey.json"; then
    echo "❌ serviceAccountKey.json STILL staged"
    ISSUES=1
else
    echo "✅ serviceAccountKey.json NOT staged"
fi

echo ""
echo "Total files staged: $(git diff --cached --name-only | wc -l)"
echo ""

if [ $ISSUES -eq 0 ]; then
    echo "=========================================="
    echo "✅✅✅ SUCCESS! Ready to commit! ✅✅✅"
    echo "=========================================="
    echo ""
    echo "Next command:"
    echo 'git commit -m "Initial commit: Pet Adoption Android App"'
else
    echo "=========================================="
    echo "❌ Still have issues - checking why..."
    echo "=========================================="
    echo ""
    echo "Staged sensitive files:"
    git diff --cached --name-only | grep -E "(google-services|local.properties|serviceAccountKey)"
fi

