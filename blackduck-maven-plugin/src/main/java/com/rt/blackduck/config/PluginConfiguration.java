package com.rt.blackduck.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Main configuration class that combines all configuration sections
 */
public class PluginConfiguration {
    
    @JsonProperty("blackduck")
    private BlackDuckConfiguration blackDuck = new BlackDuckConfiguration();
    
    @JsonProperty("schedule")
    private ScheduleConfiguration schedule = new ScheduleConfiguration();
    
    @JsonProperty("git")
    private GitConfiguration git = new GitConfiguration();
    
    @JsonProperty("auto-fix")
    private AutoFixConfiguration autoFix = new AutoFixConfiguration();

    // Getters and Setters
    public BlackDuckConfiguration getBlackDuck() {
        return blackDuck;
    }

    public void setBlackDuck(BlackDuckConfiguration blackDuck) {
        this.blackDuck = blackDuck;
    }

    public ScheduleConfiguration getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleConfiguration schedule) {
        this.schedule = schedule;
    }

    public GitConfiguration getGit() {
        return git;
    }

    public void setGit(GitConfiguration git) {
        this.git = git;
    }

    public AutoFixConfiguration getAutoFix() {
        return autoFix;
    }

    public void setAutoFix(AutoFixConfiguration autoFix) {
        this.autoFix = autoFix;
    }
    
    /**
     * Configuration for automatic fixing behavior
     */
    public static class AutoFixConfiguration {
        
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("update-direct-dependencies")
        private boolean updateDirectDependencies = true;
        
        @JsonProperty("add-exclusions")
        private boolean addExclusions = true;
        
        @JsonProperty("add-direct-replacements")
        private boolean addDirectReplacements = true;
        
        @JsonProperty("backup-pom")
        private boolean backupPom = true;
        
        @JsonProperty("max-version-jump")
        private String maxVersionJump = "MINOR"; // PATCH, MINOR, MAJOR
        
        @JsonProperty("exclude-snapshot-versions")
        private boolean excludeSnapshotVersions = true;
        
        @JsonProperty("prefer-release-versions")
        private boolean preferReleaseVersions = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isUpdateDirectDependencies() {
            return updateDirectDependencies;
        }

        public void setUpdateDirectDependencies(boolean updateDirectDependencies) {
            this.updateDirectDependencies = updateDirectDependencies;
        }

        public boolean isAddExclusions() {
            return addExclusions;
        }

        public void setAddExclusions(boolean addExclusions) {
            this.addExclusions = addExclusions;
        }

        public boolean isAddDirectReplacements() {
            return addDirectReplacements;
        }

        public void setAddDirectReplacements(boolean addDirectReplacements) {
            this.addDirectReplacements = addDirectReplacements;
        }

        public boolean isBackupPom() {
            return backupPom;
        }

        public void setBackupPom(boolean backupPom) {
            this.backupPom = backupPom;
        }

        public String getMaxVersionJump() {
            return maxVersionJump;
        }

        public void setMaxVersionJump(String maxVersionJump) {
            this.maxVersionJump = maxVersionJump;
        }

        public boolean isExcludeSnapshotVersions() {
            return excludeSnapshotVersions;
        }

        public void setExcludeSnapshotVersions(boolean excludeSnapshotVersions) {
            this.excludeSnapshotVersions = excludeSnapshotVersions;
        }

        public boolean isPreferReleaseVersions() {
            return preferReleaseVersions;
        }

        public void setPreferReleaseVersions(boolean preferReleaseVersions) {
            this.preferReleaseVersions = preferReleaseVersions;
        }
    }
}