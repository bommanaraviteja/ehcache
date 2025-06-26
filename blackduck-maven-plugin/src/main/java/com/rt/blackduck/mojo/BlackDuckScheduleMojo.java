package com.rt.blackduck.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rt.blackduck.config.PluginConfiguration;
import com.rt.blackduck.service.SchedulerService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Maven MOJO for starting the Black Duck scheduler
 */
@Mojo(name = "schedule")
public class BlackDuckScheduleMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "blackduck.configFile", defaultValue = "${project.basedir}/blackduck-config.yml")
    private File configFile;

    @Parameter(property = "blackduck.daemon", defaultValue = "false")
    private boolean daemon;

    @Parameter(property = "blackduck.cronExpression")
    private String cronExpression;

    @Parameter(property = "blackduck.timezone", defaultValue = "UTC")
    private String timezone;

    @Parameter(property = "blackduck.runOnStartup", defaultValue = "false")
    private boolean runOnStartup;

    private SchedulerService schedulerService;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting Black Duck scheduler for project: " + project.getName());

        try {
            // Load configuration
            PluginConfiguration config = loadConfiguration();

            // Override with command line parameters if provided
            overrideConfigWithParameters(config);

            // Validate configuration
            validateConfiguration(config);

            // Start scheduler
            startScheduler(config);

        } catch (Exception e) {
            getLog().error("Failed to start Black Duck scheduler", e);
            throw new MojoExecutionException("Failed to start Black Duck scheduler", e);
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
        if (cronExpression != null) {
            config.getSchedule().setCronExpression(cronExpression);
        }
        if (timezone != null) {
            config.getSchedule().setTimezone(timezone);
        }
        config.getSchedule().setRunOnStartup(runOnStartup);
        config.getSchedule().setEnabled(true); // Enable scheduling when this goal is run
    }

    private void validateConfiguration(PluginConfiguration config) throws MojoExecutionException {
        if (config.getBlackDuck().getBlackDuckUrl() == null || config.getBlackDuck().getBlackDuckUrl().trim().isEmpty()) {
            throw new MojoExecutionException("Black Duck URL is required. Set it in configuration file.");
        }
        if (config.getBlackDuck().getApiToken() == null || config.getBlackDuck().getApiToken().trim().isEmpty()) {
            throw new MojoExecutionException("Black Duck API token is required. Set it in configuration file.");
        }
        if (config.getGit().getGithubToken() == null || config.getGit().getGithubToken().trim().isEmpty()) {
            throw new MojoExecutionException("GitHub token is required for PR creation. Set it in configuration file.");
        }
        if (config.getGit().getRepositoryUrl() == null || config.getGit().getRepositoryUrl().trim().isEmpty()) {
            throw new MojoExecutionException("Repository URL is required for PR creation. Set it in configuration file.");
        }
    }

    private void startScheduler(PluginConfiguration config) throws Exception {
        File projectDirectory = project.getBasedir();

        // Create and start scheduler service
        schedulerService = new SchedulerService(config.getSchedule(), config, projectDirectory);
        schedulerService.start();

        getLog().info("Black Duck scheduler started successfully");
        getLog().info("Cron expression: " + config.getSchedule().getCronExpression());
        getLog().info("Timezone: " + config.getSchedule().getTimezone());

        if (daemon) {
            getLog().info("Running in daemon mode. The scheduler will continue running...");
            getLog().info("Press Ctrl+C to stop the scheduler");

            // Add shutdown hook to stop scheduler gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                getLog().info("Shutting down Black Duck scheduler...");
                if (schedulerService != null) {
                    schedulerService.stop();
                }
            }));

            // Keep the process alive
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                getLog().info("Scheduler interrupted, shutting down...");
                Thread.currentThread().interrupt();
            }
        } else {
            getLog().info("Running in interactive mode. Commands:");
            getLog().info("  'trigger' - Trigger a manual scan");
            getLog().info("  'status'  - Show scheduler status");
            getLog().info("  'quit'    - Stop scheduler and exit");

            // Interactive mode
            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.print("blackduck> ");
                String command = scanner.nextLine().trim().toLowerCase();

                switch (command) {
                    case "trigger":
                        getLog().info("Triggering manual scan...");
                        schedulerService.triggerManualScan();
                        break;
                    case "status":
                        getLog().info("Scheduler is running");
                        getLog().info("Next scheduled run: " + config.getSchedule().getCronExpression());
                        break;
                    case "quit":
                    case "exit":
                        running = false;
                        break;
                    case "help":
                        getLog().info("Available commands: trigger, status, quit");
                        break;
                    default:
                        getLog().warn("Unknown command: " + command + ". Type 'help' for available commands.");
                        break;
                }
            }

            scanner.close();
        }

        // Stop scheduler
        if (schedulerService != null) {
            schedulerService.stop();
        }
    }
}