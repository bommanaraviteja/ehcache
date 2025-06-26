package com.rt.blackduck.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration class for Black Duck settings
 */
public class BlackDuckConfiguration {
    
    @JsonProperty("url")
    private String blackDuckUrl;
    
    @JsonProperty("token")
    private String apiToken;
    
    @JsonProperty("project-name")
    private String projectName;
    
    @JsonProperty("project-version")
    private String projectVersion;
    
    @JsonProperty("timeout")
    private int timeoutMinutes = 30;
    
    @JsonProperty("trust-cert")
    private boolean trustCert = false;
    
    @JsonProperty("scan-paths")
    private String[] scanPaths = {"."};
    
    @JsonProperty("exclude-patterns")
    private String[] excludePatterns = {"target/**", "node_modules/**", ".git/**"};

    // Getters and Setters
    public String getBlackDuckUrl() {
        return blackDuckUrl;
    }

    public void setBlackDuckUrl(String blackDuckUrl) {
        this.blackDuckUrl = blackDuckUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public boolean isTrustCert() {
        return trustCert;
    }

    public void setTrustCert(boolean trustCert) {
        this.trustCert = trustCert;
    }

    public String[] getScanPaths() {
        return scanPaths;
    }

    public void setScanPaths(String[] scanPaths) {
        this.scanPaths = scanPaths;
    }

    public String[] getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(String[] excludePatterns) {
        this.excludePatterns = excludePatterns;
    }
}