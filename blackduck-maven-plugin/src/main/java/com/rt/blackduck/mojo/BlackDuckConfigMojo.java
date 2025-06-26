package com.rt.blackduck.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rt.blackduck.config.PluginConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

/**
 * Maven MOJO for generating Black Duck configuration file
 */
@Mojo(name = "generate-config")
public class BlackDuckConfigMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "blackduck.configFile", defaultValue = "${project.basedir}/blackduck-config.yml")
    private File configFile;

    @Parameter(property = "blackduck.overwrite", defaultValue = "false")
    private boolean overwrite;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Black Duck configuration file: " + configFile.getAbsolutePath());

        if (configFile.exists() && !overwrite) {
            getLog().warn("Configuration file already exists: " + configFile.getAbsolutePath());
            getLog().warn("Use -Dblackduck.overwrite=true to overwrite the existing file");
            return;
        }

        try {
            // Create default configuration
            PluginConfiguration config = createDefaultConfiguration();

            // Write configuration to file
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);

            getLog().info("Successfully generated configuration file: " + configFile.getAbsolutePath());
            getLog().info("");
            getLog().info("Please update the following required settings:");
            getLog().info("  blackduck.url - Your Black Duck Hub URL");
            getLog().info("  blackduck.token - Your Black Duck API token");
            getLog().info("  git.github-token - Your GitHub personal access token");
            getLog().info("  git.repository-url - Your GitHub repository URL");
            getLog().info("");
            getLog().info("Optional settings you may want to customize:");
            getLog().info("  schedule.cron-expression - When to run scans (default: daily at 2 AM)");
            getLog().info("  schedule.timezone - Timezone for scheduling (default: UTC)");
            getLog().info("  git.target-branch - Target branch for PRs (default: main)");
            getLog().info("  auto-fix.max-version-jump - Maximum version update allowed (PATCH/MINOR/MAJOR)");

        } catch (IOException e) {
            getLog().error("Failed to generate configuration file", e);
            throw new MojoExecutionException("Failed to generate configuration file", e);
        }
    }

    private PluginConfiguration createDefaultConfiguration() {
        PluginConfiguration config = new PluginConfiguration();

        // Black Duck configuration
        config.getBlackDuck().setBlackDuckUrl("https://your-blackduck-instance.com");
        config.getBlackDuck().setApiToken("your-blackduck-api-token");
        config.getBlackDuck().setProjectName(project.getName());
        config.getBlackDuck().setProjectVersion(project.getVersion());
        config.getBlackDuck().setTimeoutMinutes(30);
        config.getBlackDuck().setTrustCert(false);
        config.getBlackDuck().setScanPaths(new String[]{"."});
        config.getBlackDuck().setExcludePatterns(new String[]{"target/**", "node_modules/**", ".git/**"});

        // Schedule configuration
        config.getSchedule().setEnabled(false);
        config.getSchedule().setCronExpression("0 0 2 * * ?"); // Daily at 2 AM
        config.getSchedule().setTimezone("UTC");
        config.getSchedule().setRunOnStartup(false);
        config.getSchedule().setMaxConcurrentJobs(1);

        // Git configuration
        config.getGit().setRepositoryUrl("https://github.com/your-org/your-repo.git");
        config.getGit().setGithubToken("your-github-token");
        config.getGit().setTargetBranch("main");
        config.getGit().setPrBranchPrefix("blackduck/auto-fix");
        config.getGit().setPrTitleTemplate("Black Duck Auto-Fix: Security vulnerabilities found on {date}");
        config.getGit().setPrBodyTemplate("""
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
                """);
        config.getGit().setAutoMerge(false);
        config.getGit().setDeleteBranchAfterMerge(true);
        config.getGit().setReviewers(new String[]{});
        config.getGit().setLabels(new String[]{"security", "auto-generated"});

        // Auto-fix configuration
        config.getAutoFix().setEnabled(true);
        config.getAutoFix().setUpdateDirectDependencies(true);
        config.getAutoFix().setAddExclusions(true);
        config.getAutoFix().setAddDirectReplacements(true);
        config.getAutoFix().setBackupPom(true);
        config.getAutoFix().setMaxVersionJump("MINOR");
        config.getAutoFix().setExcludeSnapshotVersions(true);
        config.getAutoFix().setPreferReleaseVersions(true);

        return config;
    }
}