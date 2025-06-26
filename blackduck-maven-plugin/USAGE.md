# Black Duck Maven Plugin - Usage Guide

This guide demonstrates how to use the Black Duck Maven Plugin to automatically scan for security vulnerabilities and create pull requests with fixes.

## Prerequisites

1. **Java 17 or higher**
2. **Maven 3.6 or higher**
3. **Black Duck Hub instance** with API access
4. **GitHub repository** with appropriate permissions
5. **GitHub Personal Access Token** with repository write permissions

## Installation

### Option 1: Install from Source

```bash
git clone <repository-url>
cd blackduck-maven-plugin
mvn clean install
```

### Option 2: Add to Your Project

Add the plugin to your project's `pom.xml`:

```xml
<plugin>
    <groupId>com.rt.blackduck</groupId>
    <artifactId>blackduck-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</plugin>
```

## Quick Start

### 1. Generate Configuration

Navigate to your project directory and generate a configuration file:

```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:generate-config
```

This creates a `blackduck-config.yml` file with default settings.

### 2. Configure Required Settings

Edit the generated `blackduck-config.yml` file:

```yaml
blackduck:
  url: "https://your-blackduck-instance.com"
  token: "your-blackduck-api-token"
  project-name: "your-project-name"
  project-version: "1.0.0"

git:
  repository-url: "https://github.com/your-org/your-repo.git"
  github-token: "your-github-token"
  target-branch: "main"
```

### 3. Run a Manual Scan

```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan
```

### 4. Start Scheduled Scans

```bash
# Interactive mode
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:schedule

# Daemon mode (runs in background)
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:schedule -Dblackduck.daemon=true
```

## Command Line Usage

### Scan Command

Run a one-time security scan:

```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan [options]
```

**Options:**
- `-Dblackduck.url=<url>` - Black Duck Hub URL
- `-Dblackduck.token=<token>` - Black Duck API token
- `-Dblackduck.projectName=<name>` - Project name
- `-Dblackduck.projectVersion=<version>` - Project version
- `-Dblackduck.autoFix=<true|false>` - Enable auto-fixing (default: true)
- `-Dblackduck.createPR=<true|false>` - Create pull request (default: true)
- `-Dgithub.token=<token>` - GitHub personal access token
- `-Dgithub.repositoryUrl=<url>` - GitHub repository URL
- `-Dgithub.targetBranch=<branch>` - Target branch for PRs (default: main)

**Example:**
```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan \
  -Dblackduck.url=https://blackduck.company.com \
  -Dblackduck.token=your-token \
  -Dgithub.token=your-github-token \
  -Dgithub.repositoryUrl=https://github.com/company/project.git
```

### Schedule Command

Start the scheduler for automated scans:

```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:schedule [options]
```

**Options:**
- `-Dblackduck.daemon=<true|false>` - Run in daemon mode (default: false)
- `-Dblackduck.cronExpression=<expression>` - Override cron expression
- `-Dblackduck.timezone=<timezone>` - Override timezone
- `-Dblackduck.runOnStartup=<true|false>` - Run scan immediately on startup

**Examples:**
```bash
# Interactive mode with custom schedule
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:schedule \
  -Dblackduck.cronExpression="0 0 */6 * * ?"

# Daemon mode (runs every 6 hours)
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:schedule \
  -Dblackduck.daemon=true \
  -Dblackduck.cronExpression="0 0 */6 * * ?"
```

### Generate Config Command

Generate a default configuration file:

```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:generate-config [options]
```

**Options:**
- `-Dblackduck.configFile=<path>` - Configuration file path (default: blackduck-config.yml)
- `-Dblackduck.overwrite=<true|false>` - Overwrite existing file (default: false)

## Configuration Examples

### Basic Configuration

```yaml
blackduck:
  url: "https://blackduck.company.com"
  token: "your-api-token"
  project-name: "my-project"
  project-version: "1.0.0"

git:
  repository-url: "https://github.com/company/my-project.git"
  github-token: "your-github-token"
  target-branch: "main"

auto-fix:
  enabled: true
  max-version-jump: "MINOR"
```

### Advanced Configuration with Scheduling

```yaml
blackduck:
  url: "https://blackduck.company.com"
  token: "your-api-token"
  project-name: "my-project"
  project-version: "1.0.0"
  timeout: 45
  exclude-patterns:
    - "target/**"
    - "node_modules/**"
    - ".git/**"
    - "*.log"

schedule:
  enabled: true
  cron-expression: "0 0 2 * * ?" # Daily at 2 AM
  timezone: "America/New_York"
  run-on-startup: false

git:
  repository-url: "https://github.com/company/my-project.git"
  github-token: "your-github-token"
  target-branch: "develop"
  pr-branch-prefix: "security/blackduck-fix"
  reviewers:
    - "security-team"
    - "lead-developer"
  labels:
    - "security"
    - "auto-generated"
    - "high-priority"

auto-fix:
  enabled: true
  update-direct-dependencies: true
  add-exclusions: true
  add-direct-replacements: true
  backup-pom: true
  max-version-jump: "MINOR"
  exclude-snapshot-versions: true
```

## Workflow Examples

### CI/CD Integration

Add to your CI/CD pipeline:

```yaml
# GitHub Actions example
- name: Run Black Duck Security Scan
  run: |
    mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan \
      -Dblackduck.url=${{ secrets.BLACKDUCK_URL }} \
      -Dblackduck.token=${{ secrets.BLACKDUCK_TOKEN }} \
      -Dgithub.token=${{ secrets.GITHUB_TOKEN }} \
      -Dgithub.repositoryUrl=${{ github.repository }}
```

### Scheduled Scans with Docker

```dockerfile
FROM openjdk:17-jdk-slim

# Install Maven
RUN apt-get update && apt-get install -y maven git

# Copy project
COPY . /app
WORKDIR /app

# Run scheduled scans
CMD ["mvn", "com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:schedule", "-Dblackduck.daemon=true"]
```

### Manual Security Review Process

1. **Run scan manually before releases:**
   ```bash
   mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan -Dblackduck.createPR=false
   ```

2. **Review findings and apply fixes manually**

3. **Create PR with fixes:**
   ```bash
   mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan -Dblackduck.autoFix=false -Dblackduck.createPR=true
   ```

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Verify Black Duck URL and API token
   - Check GitHub token permissions
   - Ensure tokens are not expired

2. **Network Issues**
   - Check firewall settings
   - Verify proxy configuration if needed
   - Use `-Dblackduck.trustCert=true` for self-signed certificates

3. **Permission Issues**
   - Ensure GitHub token has repository write permissions
   - Check if branch protection rules allow automated commits
   - Verify Black Duck project permissions

4. **Build Failures**
   - Check Java version (requires 17+)
   - Verify Maven version (requires 3.6+)
   - Review dependency conflicts

### Debug Mode

Enable debug logging:

```bash
mvn com.rt.blackduck:blackduck-maven-plugin:1.0.0-SNAPSHOT:scan -X
```

### Log Files

Check log files in the `logs/` directory:
- `blackduck-plugin.log` - Main plugin log
- `blackduck-plugin.YYYY-MM-DD.log` - Daily log files

## Best Practices

1. **Security**
   - Store sensitive tokens in environment variables
   - Use GitHub personal access tokens with minimal required permissions
   - Regularly rotate API tokens

2. **Configuration**
   - Start with conservative auto-fix settings
   - Test in development environment first
   - Configure appropriate reviewers for security PRs

3. **Scheduling**
   - Schedule scans during low-activity periods
   - Consider time zones for distributed teams
   - Monitor scan duration and adjust timeout settings

4. **Review Process**
   - Always review auto-generated PRs before merging
   - Test changes in staging environment
   - Document security fixes for audit trails

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review log files for error details
3. Create an issue in the project repository
4. Contact your security team for Black Duck-specific questions