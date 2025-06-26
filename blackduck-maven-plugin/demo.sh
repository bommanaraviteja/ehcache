#!/bin/bash

# Black Duck Maven Plugin Demo Script
# This script demonstrates the plugin functionality using the ehcache demo project

set -e

echo "=========================================="
echo "Black Duck Maven Plugin Demo"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Plugin coordinates
PLUGIN="com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT"

# Demo project path
DEMO_PROJECT="../ehcacheDemo/ehcache"

echo -e "${BLUE}Step 1: Building the Black Duck Maven Plugin${NC}"
echo "Building plugin from source..."
mvn clean install -q
echo -e "${GREEN}✓ Plugin built successfully${NC}"
echo

echo -e "${BLUE}Step 2: Setting up demo project${NC}"
if [ ! -d "$DEMO_PROJECT" ]; then
    echo -e "${RED}✗ Demo project not found at $DEMO_PROJECT${NC}"
    exit 1
fi

cd "$DEMO_PROJECT"
echo "Working with demo project: $(pwd)"
echo -e "${GREEN}✓ Demo project found${NC}"
echo

echo -e "${BLUE}Step 3: Generating configuration file${NC}"
echo "Generating blackduck-config.yml..."
mvn $PLUGIN:generate-config -q
echo -e "${GREEN}✓ Configuration file generated${NC}"
echo

echo -e "${BLUE}Step 4: Showing current POM dependencies${NC}"
echo "Current vulnerable dependencies in pom.xml:"
echo -e "${YELLOW}"
grep -A 3 -B 1 "hibernate-core\|mysql-connector-java" pom.xml || true
echo -e "${NC}"

echo -e "${BLUE}Step 5: Running Black Duck scan (simulation)${NC}"
echo "Running scan with mock data (no actual Black Duck connection)..."
echo -e "${YELLOW}Note: This is a simulation - in real usage you would provide:${NC}"
echo -e "${YELLOW}- Black Duck URL and API token${NC}"
echo -e "${YELLOW}- GitHub repository URL and token${NC}"
echo

# Run scan without actual Black Duck connection (will fail gracefully)
echo "Attempting scan..."
mvn $PLUGIN:scan \
  -Dblackduck.url=https://demo-blackduck.com \
  -Dblackduck.token=demo-token \
  -Dblackduck.projectName="EHCache Demo" \
  -Dblackduck.projectVersion="1.0.0" \
  -Dblackduck.autoFix=true \
  -Dblackduck.createPR=false \
  -Dgithub.token=demo-token \
  -Dgithub.repositoryUrl=https://github.com/demo/ehcache.git 2>&1 | head -20 || true

echo -e "${YELLOW}Expected: Scan would fail due to invalid credentials (this is normal for demo)${NC}"
echo

echo -e "${BLUE}Step 6: Demonstrating configuration options${NC}"
echo "Generated configuration file contents:"
echo -e "${YELLOW}"
cat blackduck-config.yml
echo -e "${NC}"

echo -e "${BLUE}Step 7: Plugin goals available${NC}"
echo "Available plugin goals:"
echo -e "${GREEN}• generate-config${NC} - Generate configuration file"
echo -e "${GREEN}• scan${NC}           - Run one-time security scan"
echo -e "${GREEN}• schedule${NC}       - Start scheduled scans"
echo

echo -e "${BLUE}Step 8: Example usage commands${NC}"
echo "Real-world usage examples:"
echo
echo -e "${GREEN}1. Generate config:${NC}"
echo "   mvn $PLUGIN:generate-config"
echo
echo -e "${GREEN}2. Run manual scan:${NC}"
echo "   mvn $PLUGIN:scan \\"
echo "     -Dblackduck.url=https://your-blackduck.com \\"
echo "     -Dblackduck.token=your-token \\"
echo "     -Dgithub.token=your-github-token"
echo
echo -e "${GREEN}3. Start scheduler (interactive):${NC}"
echo "   mvn $PLUGIN:schedule"
echo
echo -e "${GREEN}4. Start scheduler (daemon):${NC}"
echo "   mvn $PLUGIN:schedule -Dblackduck.daemon=true"
echo

echo -e "${BLUE}Step 9: What the plugin would do with real credentials${NC}"
echo -e "${GREEN}With valid Black Duck and GitHub credentials, the plugin would:${NC}"
echo "1. 🔍 Scan the project for security vulnerabilities"
echo "2. 📋 Identify vulnerable dependencies (e.g., old Hibernate, MySQL connector)"
echo "3. 🔧 Auto-fix POM by:"
echo "   - Updating direct dependencies to secure versions"
echo "   - Adding exclusions for vulnerable transitive dependencies"
echo "   - Adding direct secure dependencies to replace excluded ones"
echo "4. 💾 Create backup of original POM"
echo "5. 🌿 Create new Git branch with fixes"
echo "6. 📤 Create pull request with detailed description"
echo "7. 👥 Request reviews from configured team members"
echo "8. 🏷️  Add security labels to the PR"
echo

echo -e "${BLUE}Step 10: Cleanup${NC}"
echo "Cleaning up demo files..."
rm -f blackduck-config.yml
echo -e "${GREEN}✓ Demo completed successfully${NC}"
echo

echo "=========================================="
echo -e "${GREEN}Demo Summary${NC}"
echo "=========================================="
echo "✓ Plugin built and installed"
echo "✓ Configuration generation demonstrated"
echo "✓ Scan command tested (simulation)"
echo "✓ All plugin goals available"
echo
echo -e "${YELLOW}Next steps for real usage:${NC}"
echo "1. Set up Black Duck Hub instance"
echo "2. Generate GitHub personal access token"
echo "3. Configure blackduck-config.yml with real credentials"
echo "4. Run actual scans on your projects"
echo
echo -e "${BLUE}For detailed usage instructions, see USAGE.md${NC}"
echo "=========================================="