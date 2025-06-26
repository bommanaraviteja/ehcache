package com.rt.blackduck.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration class for Git and PR settings
 */
public class GitConfiguration {
    
    @JsonProperty("repository-url")
    private String repositoryUrl;
    
    @JsonProperty("github-token")
    private String githubToken;
    
    @JsonProperty("target-branch")
    private String targetBranch = "main";
    
    @JsonProperty("pr-branch-prefix")
    private String prBranchPrefix = "blackduck/auto-fix";
    
    @JsonProperty("pr-title-template")
    private String prTitleTemplate = "Black Duck Auto-Fix: Security vulnerabilities found on {date}";
    
    @JsonProperty("pr-body-template")
    private String prBodyTemplate = """
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
        """;
    
    @JsonProperty("auto-merge")
    private boolean autoMerge = false;
    
    @JsonProperty("delete-branch-after-merge")
    private boolean deleteBranchAfterMerge = true;
    
    @JsonProperty("reviewers")
    private String[] reviewers = {};
    
    @JsonProperty("labels")
    private String[] labels = {"security", "auto-generated"};

    // Getters and Setters
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getPrBranchPrefix() {
        return prBranchPrefix;
    }

    public void setPrBranchPrefix(String prBranchPrefix) {
        this.prBranchPrefix = prBranchPrefix;
    }

    public String getPrTitleTemplate() {
        return prTitleTemplate;
    }

    public void setPrTitleTemplate(String prTitleTemplate) {
        this.prTitleTemplate = prTitleTemplate;
    }

    public String getPrBodyTemplate() {
        return prBodyTemplate;
    }

    public void setPrBodyTemplate(String prBodyTemplate) {
        this.prBodyTemplate = prBodyTemplate;
    }

    public boolean isAutoMerge() {
        return autoMerge;
    }

    public void setAutoMerge(boolean autoMerge) {
        this.autoMerge = autoMerge;
    }

    public boolean isDeleteBranchAfterMerge() {
        return deleteBranchAfterMerge;
    }

    public void setDeleteBranchAfterMerge(boolean deleteBranchAfterMerge) {
        this.deleteBranchAfterMerge = deleteBranchAfterMerge;
    }

    public String[] getReviewers() {
        return reviewers;
    }

    public void setReviewers(String[] reviewers) {
        this.reviewers = reviewers;
    }

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }
}