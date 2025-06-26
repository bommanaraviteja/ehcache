package com.rt.blackduck.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rt.blackduck.config.PluginConfiguration;
import com.rt.blackduck.service.BlackDuckService;
import com.rt.blackduck.service.GitService;
import com.rt.blackduck.util.PomManipulator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

/**
 * Maven MOJO for running Black Duck scan and auto-fix
 */
@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY)
public class BlackDuckScanMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "blackduck.configFile", defaultValue = "${project.basedir}/blackduck-config.yml")
    private File configFile;

    @Parameter(property = "blackduck.url")
    private String blackDuckUrl;

    @Parameter(property = "blackduck.token")
    private String apiToken;

    @Parameter(property = "blackduck.projectName")
    private String projectName;

    @Parameter(property = "blackduck.projectVersion")
    private String projectVersion;

    @Parameter(property = "blackduck.autoFix", defaultValue = "true")
    private boolean autoFix;

    @Parameter(property = "blackduck.createPR", defaultValue = "true")
    private boolean createPR;

    @Parameter(property = "github.token")
    private String githubToken;

    @Parameter(property = "github.repositoryUrl")
    private String repositoryUrl;

    @Parameter(property = "github.targetBranch", defaultValue = "main")
    private String targetBranch;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting Black Duck scan for project: " + project.getName());

        try {
            // Load configuration
            PluginConfiguration config = loadConfiguration();

            // Override with command line parameters if provided
            overrideConfigWithParameters(config);

            // Validate configuration
            validateConfiguration(config);

            // Execute scan workflow
            executeScanWorkflow(config);

        } catch (Exception e) {
            getLog().error("Failed to execute Black Duck scan", e);
            throw new MojoExecutionException("Failed to execute Black Duck scan", e);
        }
    }

    private PluginConfiguration loadConfiguration() throws IOException {
        PluginConfiguration config;

        if (configFile.exists()) {
            getLog().info("Loading configuration from: " + configFile.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            config = mapper.readValue(configFile, PluginConfiguration.class);
        } else {
            getLog().info("Configuration file not found, using defaults: " + configFile.getAbsolutePath());
            config = new PluginConfiguration();
        }

        return config;
    }

    private void overrideConfigWithParameters(PluginConfiguration config) {
        if (blackDuckUrl != null) {
            config.getBlackDuck().setBlackDuckUrl(blackDuckUrl);
        }
        if (apiToken != null) {
            config.getBlackDuck().setApiToken(apiToken);
        }
        if (projectName != null) {
            config.getBlackDuck().setProjectName(projectName);
        } else if (config.getBlackDuck().getProjectName() == null) {
            config.getBlackDuck().setProjectName(project.getName());
        }
        if (projectVersion != null) {
            config.getBlackDuck().setProjectVersion(projectVersion);
        } else if (config.getBlackDuck().getProjectVersion() == null) {
            config.getBlackDuck().setProjectVersion(project.getVersion());
        }
        if (githubToken != null) {
            config.getGit().setGithubToken(githubToken);
        }
        if (repositoryUrl != null) {
            config.getGit().setRepositoryUrl(repositoryUrl);
        }
        if (targetBranch != null) {
            config.getGit().setTargetBranch(targetBranch);
        }

        config.getAutoFix().setEnabled(autoFix);
    }

    private void validateConfiguration(PluginConfiguration config) throws MojoExecutionException {
        if (config.getBlackDuck().getBlackDuckUrl() == null || config.getBlackDuck().getBlackDuckUrl().trim().isEmpty()) {
            throw new MojoExecutionException("Black Duck URL is required. Set it via blackduck.url property or configuration file.");
        }
        if (config.getBlackDuck().getApiToken() == null || config.getBlackDuck().getApiToken().trim().isEmpty()) {
            throw new MojoExecutionException("Black Duck API token is required. Set it via blackduck.token property or configuration file.");
        }
        if (createPR) {
            if (config.getGit().getGithubToken() == null || config.getGit().getGithubToken().trim().isEmpty()) {
                throw new MojoExecutionException("GitHub token is required for PR creation. Set it via github.token property or configuration file.");
            }
            if (config.getGit().getRepositoryUrl() == null || config.getGit().getRepositoryUrl().trim().isEmpty()) {
                throw new MojoExecutionException("Repository URL is required for PR creation. Set it via github.repositoryUrl property or configuration file.");
            }
        }
    }

    private void executeScanWorkflow(PluginConfiguration config) throws Exception {
        File projectDirectory = project.getBasedir();

        // Initialize services
        BlackDuckService blackDuckService = new BlackDuckService(config.getBlackDuck());
        PomManipulator pomManipulator = new PomManipulator(config.getAutoFix());
        GitService gitService = createPR ? new GitService(config.getGit()) : null;

        // Step 1: Run Black Duck scan
        getLog().info("Step 1: Starting Black Duck scan");
        String scanId = blackDuckService.startScan(projectDirectory);

        // Step 2: Wait for scan completion and get results
        getLog().info("Step 2: Waiting for scan completion");
        // In a real implementation, you'd poll for scan completion
        Thread.sleep(5000); // Simulate scan time

        var vulnerabilities = blackDuckService.getScanResults(scanId);
        getLog().info("Found " + vulnerabilities.size() + " vulnerabilities");

        if (vulnerabilities.isEmpty()) {
            getLog().info("No vulnerabilities found, scan completed successfully");
            return;
        }

        // Log vulnerabilities
        for (var vulnerability : vulnerabilities) {
            getLog().warn(String.format("Vulnerability: %s (%s) in %s - Fix: %s",
                    vulnerability.getCveId(),
                    vulnerability.getSeverity(),
                    vulnerability.getComponent(),
                    vulnerability.getFixedVersion()));
        }

        if (!autoFix) {
            getLog().info("Auto-fix is disabled, skipping POM modifications");
            return;
        }

        // Step 3: Apply fixes to POM file
        getLog().info("Step 3: Applying fixes to POM file");
        File pomFile = new File(projectDirectory, "pom.xml");
        boolean pomModified = pomManipulator.applyFixes(pomFile, vulnerabilities);

        if (!pomModified) {
            getLog().info("No POM modifications were made");
            return;
        }

        getLog().info("POM file has been updated with security fixes");

        if (!createPR) {
            getLog().info("PR creation is disabled, scan completed with local changes");
            return;
        }

        // Step 4: Check if there are changes to commit
        if (gitService != null && !gitService.hasUncommittedChanges(projectDirectory)) {
            getLog().info("No uncommitted changes found, skipping PR creation");
            return;
        }

        // Step 5: Create pull request
        if (gitService != null) {
            getLog().info("Step 5: Creating pull request");
            String commitMessage = String.format("Black Duck Auto-Fix: Resolved %d security vulnerabilities", vulnerabilities.size());
            String prUrl = gitService.createPullRequest(projectDirectory, commitMessage);

            getLog().info("Successfully created pull request: " + prUrl);
        }
    }
}