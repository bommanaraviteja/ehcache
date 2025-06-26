package com.rt.blackduck.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration class for scheduling settings
 */
public class ScheduleConfiguration {
    
    @JsonProperty("enabled")
    private boolean enabled = false;
    
    @JsonProperty("cron-expression")
    private String cronExpression = "0 0 2 * * ?"; // Daily at 2 AM
    
    @JsonProperty("timezone")
    private String timezone = "UTC";
    
    @JsonProperty("run-on-startup")
    private boolean runOnStartup = false;
    
    @JsonProperty("max-concurrent-jobs")
    private int maxConcurrentJobs = 1;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public int getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    public void setMaxConcurrentJobs(int maxConcurrentJobs) {
        this.maxConcurrentJobs = maxConcurrentJobs;
    }
}