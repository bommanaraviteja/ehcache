# Black Duck Maven Plugin

A comprehensive Maven plugin for automated Black Duck security scanning with intelligent POM auto-correction and pull request creation.

## Features

- **Scheduled Security Scans**: Automatically run Black Duck scans based on configurable cron expressions
- **Intelligent POM Auto-Correction**: 
  - Update direct dependencies to secure versions
  - Add exclusions for vulnerable transitive dependencies
  - Add direct dependencies to replace excluded vulnerable ones
- **Automated Pull Request Creation**: Create PRs with security fixes to configured branches
- **Flexible Configuration**: YAML-based configuration with command-line overrides
- **Java 21 Support**: Built with modern Java features and best practices

## Quick Start

### 1. Add the Plugin to Your Project

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>com.rt.blackduck</groupId>
    <artifactId>blackduck-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</plugin>
```

### 2. Generate Configuration File

```bash
mvn blackduck:generate-config
```

This creates a `blackduck-config.yml` file with default settings that you need to customize.

### 3. Configure Required Settings

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

schedule:
  enabled: true
  cron-expression: "0 0 2 * * ?" # Daily at 2 AM
  timezone: "UTC"
```

### 4. Run a Manual Scan

```bash
mvn blackduck:scan
```

### 5. Start the Scheduler

```bash
mvn blackduck:schedule -Dblackduck.daemon=true
```

## Goals

### `blackduck:scan`

Runs a one-time Black Duck scan and applies auto-fixes.

**Parameters:**
- `blackduck.url` - Black Duck Hub URL
- `blackduck.token` - Black Duck API token
- `blackduck.projectName` - Project name (defaults to Maven project name)
- `blackduck.projectVersion` - Project version (defaults to Maven project version)
- `blackduck.autoFix` - Enable auto-fixing (default: true)
- `blackduck.createPR` - Create pull request (default: true)
- `github.token` - GitHub personal access token
- `github.repositoryUrl` - GitHub repository URL
- `github.targetBranch` - Target branch for PRs (default: main)

**Example:**
```bash
mvn blackduck:scan \
  -Dblackduck.url=https://blackduck.company.com \
  -Dblackduck.token=your-token \
  -Dgithub.token=your-github-token \
  -Dgithub.repositoryUrl=https://github.com/company/project.git
```

### `blackduck:schedule`

Starts the scheduler for automated scans.

**Parameters:**
- `blackduck.daemon` - Run in daemon mode (default: false)
- `blackduck.cronExpression` - Override cron expression
- `blackduck.timezone` - Override timezone
- `blackduck.runOnStartup` - Run scan immediately on startup

**Example:**
```bash
# Interactive mode
mvn blackduck:schedule

# Daemon mode
mvn blackduck:schedule -Dblackduck.daemon=true

# Custom schedule
mvn blackduck:schedule -Dblackduck.cronExpression="0 0 */6 * * ?" -Dblackduck.daemon=true
```

### `blackduck:generate-config`

Generates a default configuration file.

**Parameters:**
- `blackduck.configFile` - Configuration file path (default: blackduck-config.yml)
- `blackduck.overwrite` - Overwrite existing file (default: false)

## Configuration Reference

### Complete Configuration Example

```yaml
blackduck:
  url: "https://your-blackduck-instance.com"
  token: "your-blackduck-api-token"
  project-name: "my-project"
  project-version: "1.0.0"
  timeout: 30
  trust-cert: false
  scan-paths:
    - "."
  exclude-patterns:
    - "target/**"
    - "node_modules/**"
    - ".git/**"

schedule:
  enabled: true
  cron-expression: "0 0 2 * * ?" # Daily at 2 AM
  timezone: "UTC"
  run-on-startup: false
  max-concurrent-jobs: 1

git:
  repository-url: "https://github.com/your-org/your-repo.git"
  github-token: "your-github-token"
  target-branch: "main"
  pr-branch-prefix: "blackduck/auto-fix"
  pr-title-template: "Black Duck Auto-Fix: Security vulnerabilities found on {date}"
  pr-body-template: |
    ## Black Duck Security Scan Results
    
    This PR contains automatic fixes for security vulnerabilities detected by Black Duck scan.
    
    ### Changes Made:
    - Updated direct dependencies to secure versions
    - Added exclusions for vulnerable transitive dependencies
    - Added direct dependencies to replace excluded vulnerable ones
    
    **Scan Date:** {date}
    **Project:** {project}
    **Version:** {version}
    
    Please review the changes before merging.
  auto-merge: false
  delete-branch-after-merge: true
  reviewers:
    - "security-team"
    - "lead-developer"
  labels:
    - "security"
    - "auto-generated"

auto-fix:
  enabled: true
  update-direct-dependencies: true
  add-exclusions: true
  add-direct-replacements: true
  backup-pom: true
  max-version-jump: "MINOR" # PATCH, MINOR, MAJOR
  exclude-snapshot-versions: true
  prefer-release-versions: true
```

### Configuration Sections

#### `blackduck`
Black Duck Hub connection and scan settings.

#### `schedule`
Quartz scheduler configuration for automated scans.

#### `git`
Git repository and GitHub integration settings.

#### `auto-fix`
POM auto-correction behavior settings.

## Auto-Fix Behavior

The plugin intelligently handles security vulnerabilities by:

1. **Direct Dependency Updates**: Updates vulnerable direct dependencies to secure versions
2. **Transitive Dependency Handling**: 
   - Adds exclusions for vulnerable transitive dependencies
   - Adds direct dependencies with secure versions to replace excluded ones
3. **Version Constraints**: Respects configured maximum version jump limits
4. **Backup Creation**: Creates timestamped backups of original POM files

### Example Auto-Fix

**Before:**
```xml
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>4.3.5.Final</version>
</dependency>
```

**After (Direct Update):**
```xml
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>5.6.15.Final</version>
</dependency>
```

**After (Exclusion + Direct Dependency):**
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-orm</artifactId>
    <version>5.3.21</version>
    <exclusions>
        <exclusion>
            <groupId>org.hibernate</groupId>
            <artifactId>hibernate-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Added by Black Duck Auto-Fix to resolve CVE-2023-1234 -->
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>5.6.15.Final</version>
</dependency>
```

## Cron Expression Examples

- `0 0 2 * * ?` - Daily at 2:00 AM
- `0 0 */6 * * ?` - Every 6 hours
- `0 0 9 * * MON-FRI` - Weekdays at 9:00 AM
- `0 0 0 1 * ?` - First day of every month at midnight

## Security Considerations

- Store sensitive tokens in environment variables or secure configuration management
- Use GitHub personal access tokens with minimal required permissions
- Review auto-generated PRs before merging
- Configure appropriate reviewers for security-related changes

## Requirements

- Java 21 or higher
- Maven 3.6 or higher
- Black Duck Hub instance with API access
- GitHub repository with appropriate permissions

## Building the Plugin

```bash
git clone <repository-url>
cd blackduck-maven-plugin
mvn clean install
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.