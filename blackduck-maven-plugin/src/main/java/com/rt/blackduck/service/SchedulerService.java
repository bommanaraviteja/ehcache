package com.rt.blackduck.service;

import com.rt.blackduck.config.PluginConfiguration;
import com.rt.blackduck.config.ScheduleConfiguration;
import com.rt.blackduck.util.PomManipulator;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.TimeZone;

/**
 * Service for scheduling Black Duck scans using Quartz
 */
public class SchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    private final ScheduleConfiguration config;
    private final PluginConfiguration pluginConfig;
    private final File projectDirectory;
    private Scheduler scheduler;

    public SchedulerService(ScheduleConfiguration config, PluginConfiguration pluginConfig, File projectDirectory) {
        this.config = config;
        this.pluginConfig = pluginConfig;
        this.projectDirectory = projectDirectory;
    }

    /**
     * Start the scheduler if enabled
     */
    public void start() {
        if (!config.isEnabled()) {
            logger.info("Scheduler is disabled, skipping startup");
            return;
        }

        try {
            // Create scheduler
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            // Create job detail
            JobDetail job = JobBuilder.newJob(BlackDuckScanJob.class)
                    .withIdentity("blackduck-scan", "security-scans")
                    .usingJobData("projectDirectory", projectDirectory.getAbsolutePath())
                    .usingJobData("pluginConfig", serializeConfig(pluginConfig))
                    .build();

            // Create trigger with cron expression
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("blackduck-trigger", "security-scans")
                    .withSchedule(CronScheduleBuilder.cronSchedule(config.getCronExpression())
                            .inTimeZone(TimeZone.getTimeZone(config.getTimezone())))
                    .build();

            // Schedule the job
            scheduler.scheduleJob(job, trigger);

            // Start the scheduler
            scheduler.start();

            logger.info("Scheduler started with cron expression: {} (timezone: {})", 
                config.getCronExpression(), config.getTimezone());

            // Run immediately if configured
            if (config.isRunOnStartup()) {
                logger.info("Running initial scan on startup");
                scheduler.triggerJob(job.getKey());
            }

        } catch (SchedulerException e) {
            logger.error("Failed to start scheduler", e);
            throw new RuntimeException("Failed to start scheduler", e);
        }
    }

    /**
     * Stop the scheduler
     */
    public void stop() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
                logger.info("Scheduler stopped");
            } catch (SchedulerException e) {
                logger.error("Failed to stop scheduler", e);
            }
        }
    }

    /**
     * Trigger a manual scan
     */
    public void triggerManualScan() {
        if (scheduler != null) {
            try {
                JobKey jobKey = new JobKey("blackduck-scan", "security-scans");
                scheduler.triggerJob(jobKey);
                logger.info("Triggered manual Black Duck scan");
            } catch (SchedulerException e) {
                logger.error("Failed to trigger manual scan", e);
            }
        } else {
            logger.warn("Scheduler is not running, cannot trigger manual scan");
        }
    }

    private String serializeConfig(PluginConfiguration config) {
        // In a real implementation, you'd serialize the config to JSON
        // For simplicity, we'll pass it through the job context differently
        return "serialized-config";
    }

    /**
     * Quartz Job that performs the Black Duck scan and auto-fix
     */
    public static class BlackDuckScanJob implements Job {
        
        private static final Logger logger = LoggerFactory.getLogger(BlackDuckScanJob.class);

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("Starting scheduled Black Duck scan job");

            try {
                JobDataMap dataMap = context.getJobDetail().getJobDataMap();
                String projectDirectoryPath = dataMap.getString("projectDirectory");
                File projectDirectory = new File(projectDirectoryPath);

                // Create a new instance of the scan workflow
                BlackDuckScanWorkflow workflow = new BlackDuckScanWorkflow();
                workflow.execute(projectDirectory);

                logger.info("Completed scheduled Black Duck scan job");

            } catch (Exception e) {
                logger.error("Failed to execute scheduled Black Duck scan", e);
                throw new JobExecutionException("Failed to execute scheduled Black Duck scan", e);
            }
        }
    }

    /**
     * Workflow that orchestrates the entire Black Duck scan and auto-fix process
     */
    public static class BlackDuckScanWorkflow {
        
        private static final Logger logger = LoggerFactory.getLogger(BlackDuckScanWorkflow.class);

        public void execute(File projectDirectory) {
            logger.info("Executing Black Duck scan workflow for directory: {}", projectDirectory.getAbsolutePath());

            try {
                // Load configuration (in a real implementation, this would be loaded from file)
                PluginConfiguration config = createDefaultConfiguration();

                // Initialize services
                BlackDuckService blackDuckService = new BlackDuckService(config.getBlackDuck());
                PomManipulator pomManipulator = new PomManipulator(config.getAutoFix());
                GitService gitService = new GitService(config.getGit());

                // Step 1: Run Black Duck scan
                logger.info("Step 1: Starting Black Duck scan");
                String scanId = blackDuckService.startScan(projectDirectory);
                
                // Step 2: Wait for scan completion and get results
                logger.info("Step 2: Waiting for scan completion");
                // In a real implementation, you'd poll for scan completion
                Thread.sleep(5000); // Simulate scan time
                
                var vulnerabilities = blackDuckService.getScanResults(scanId);
                logger.info("Found {} vulnerabilities", vulnerabilities.size());

                if (vulnerabilities.isEmpty()) {
                    logger.info("No vulnerabilities found, skipping auto-fix");
                    return;
                }

                // Step 3: Apply fixes to POM file
                logger.info("Step 3: Applying fixes to POM file");
                File pomFile = new File(projectDirectory, "pom.xml");
                boolean pomModified = pomManipulator.applyFixes(pomFile, vulnerabilities);

                if (!pomModified) {
                    logger.info("No POM modifications were made, skipping PR creation");
                    return;
                }

                // Step 4: Check if there are changes to commit
                if (!gitService.hasUncommittedChanges(projectDirectory)) {
                    logger.info("No uncommitted changes found, skipping PR creation");
                    return;
                }

                // Step 5: Create pull request
                logger.info("Step 5: Creating pull request");
                String commitMessage = String.format("Black Duck Auto-Fix: Resolved %d security vulnerabilities", vulnerabilities.size());
                String prUrl = gitService.createPullRequest(projectDirectory, commitMessage);
                
                logger.info("Successfully created pull request: {}", prUrl);

            } catch (Exception e) {
                logger.error("Failed to execute Black Duck scan workflow", e);
                throw new RuntimeException("Failed to execute Black Duck scan workflow", e);
            }
        }

        private PluginConfiguration createDefaultConfiguration() {
            // In a real implementation, this would load from configuration files
            PluginConfiguration config = new PluginConfiguration();
            
            // Set default values - these would typically come from plugin configuration
            config.getBlackDuck().setBlackDuckUrl("https://your-blackduck-instance.com");
            config.getBlackDuck().setApiToken("your-api-token");
            config.getBlackDuck().setProjectName("default-project");
            config.getBlackDuck().setProjectVersion("1.0.0");
            
            config.getGit().setRepositoryUrl("https://github.com/your-org/your-repo.git");
            config.getGit().setGithubToken("your-github-token");
            config.getGit().setTargetBranch("main");
            
            return config;
        }
    }
}