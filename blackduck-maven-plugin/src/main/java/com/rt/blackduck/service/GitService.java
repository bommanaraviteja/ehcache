package com.rt.blackduck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rt.blackduck.config.GitConfiguration;
import okhttp3.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Service for Git operations and GitHub PR creation
 */
public class GitService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitService.class);
    
    private final GitConfiguration config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitService(GitConfiguration config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Create a new branch, commit changes, and create a pull request
     */
    public String createPullRequest(File projectDirectory, String commitMessage) {
        try {
            // Initialize Git repository
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(projectDirectory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            try (Git git = new Git(repository)) {
                // Create and checkout new branch
                String branchName = createBranchName();
                git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .call();

                logger.info("Created and checked out branch: {}", branchName);

                // Add all changes
                git.add()
                    .addFilepattern(".")
                    .call();

                // Commit changes
                git.commit()
                    .setMessage(commitMessage)
                    .setAuthor("Black Duck Auto-Fix", "blackduck@auto-fix.com")
                    .call();

                logger.info("Committed changes with message: {}", commitMessage);

                // Push branch to remote
                git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", config.getGithubToken()))
                    .setRemote("origin")
                    .add(branchName)
                    .call();

                logger.info("Pushed branch {} to remote", branchName);

                // Create pull request via GitHub API
                return createGitHubPullRequest(branchName);

            }
        } catch (Exception e) {
            logger.error("Failed to create pull request", e);
            throw new RuntimeException("Failed to create pull request", e);
        }
    }

    private String createBranchName() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return config.getPrBranchPrefix() + "/" + timestamp;
    }

    private String createGitHubPullRequest(String branchName) throws IOException {
        String repoPath = extractRepoPath(config.getRepositoryUrl());
        String apiUrl = "https://api.github.com/repos/" + repoPath + "/pulls";

        String title = formatTemplate(config.getPrTitleTemplate());
        String body = formatTemplate(config.getPrBodyTemplate());

        String prJson = String.format("""
            {
                "title": "%s",
                "body": "%s",
                "head": "%s",
                "base": "%s",
                "draft": false
            }
            """, 
            escapeJson(title), 
            escapeJson(body), 
            branchName, 
            config.getTargetBranch());

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), prJson);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader("Authorization", "token " + config.getGithubToken())
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode prResponse = objectMapper.readTree(response.body().string());
                String prUrl = prResponse.get("html_url").asText();
                int prNumber = prResponse.get("number").asInt();

                logger.info("Created pull request #{}: {}", prNumber, prUrl);

                // Add labels if configured
                if (config.getLabels().length > 0) {
                    addLabelsToPR(repoPath, prNumber);
                }

                // Request reviewers if configured
                if (config.getReviewers().length > 0) {
                    requestReviewers(repoPath, prNumber);
                }

                return prUrl;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                logger.error("Failed to create pull request: {} - {}", response.message(), errorBody);
                throw new RuntimeException("Failed to create pull request: " + response.message());
            }
        }
    }

    private void addLabelsToPR(String repoPath, int prNumber) {
        try {
            String apiUrl = String.format("https://api.github.com/repos/%s/issues/%d/labels", repoPath, prNumber);
            
            StringBuilder labelsJson = new StringBuilder("[");
            for (int i = 0; i < config.getLabels().length; i++) {
                if (i > 0) labelsJson.append(",");
                labelsJson.append("\"").append(config.getLabels()[i]).append("\"");
            }
            labelsJson.append("]");

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), labelsJson.toString());

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "token " + config.getGithubToken())
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Added labels to PR #{}", prNumber);
                } else {
                    logger.warn("Failed to add labels to PR #{}: {}", prNumber, response.message());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to add labels to PR #{}", prNumber, e);
        }
    }

    private void requestReviewers(String repoPath, int prNumber) {
        try {
            String apiUrl = String.format("https://api.github.com/repos/%s/pulls/%d/requested_reviewers", repoPath, prNumber);
            
            StringBuilder reviewersJson = new StringBuilder("{\"reviewers\":[");
            for (int i = 0; i < config.getReviewers().length; i++) {
                if (i > 0) reviewersJson.append(",");
                reviewersJson.append("\"").append(config.getReviewers()[i]).append("\"");
            }
            reviewersJson.append("]}");

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), reviewersJson.toString());

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "token " + config.getGithubToken())
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Requested reviewers for PR #{}", prNumber);
                } else {
                    logger.warn("Failed to request reviewers for PR #{}: {}", prNumber, response.message());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to request reviewers for PR #{}", prNumber, e);
        }
    }

    private String extractRepoPath(String repositoryUrl) {
        // Extract owner/repo from GitHub URL
        // e.g., https://github.com/owner/repo.git -> owner/repo
        String path = repositoryUrl;
        if (path.startsWith("https://github.com/")) {
            path = path.substring("https://github.com/".length());
        } else if (path.startsWith("git@github.com:")) {
            path = path.substring("git@github.com:".length());
        }
        
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        
        return path;
    }

    private String formatTemplate(String template) {
        LocalDateTime now = LocalDateTime.now();
        return template
                .replace("{date}", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .replace("{project}", "Current Project") // This could be made configurable
                .replace("{version}", "1.0.0"); // This could be extracted from POM
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Check if there are any uncommitted changes in the repository
     */
    public boolean hasUncommittedChanges(File projectDirectory) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(projectDirectory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            try (Git git = new Git(repository)) {
                return !git.status().call().isClean();
            }
        } catch (Exception e) {
            logger.error("Failed to check git status", e);
            return false;
        }
    }
}