package com.rt.blackduck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rt.blackduck.config.BlackDuckConfiguration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for interacting with Black Duck Hub API
 */
public class BlackDuckService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlackDuckService.class);
    private static final String API_VERSION = "2023-01-02";
    
    private final BlackDuckConfiguration config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public BlackDuckService(BlackDuckConfiguration config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient();
    }

    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(Duration.ofMinutes(config.getTimeoutMinutes()))
                .writeTimeout(30, TimeUnit.SECONDS);

        if (config.isTrustCert()) {
            try {
                final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
                };

                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                logger.warn("Failed to configure SSL trust all certificates", e);
            }
        }

        return builder.build();
    }

    /**
     * Authenticate with Black Duck Hub
     */
    public boolean authenticate() {
        try {
            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                String.format("{\"token\": \"%s\"}", config.getApiToken())
            );

            Request request = new Request.Builder()
                    .url(config.getBlackDuckUrl() + "/api/tokens/authenticate")
                    .post(body)
                    .addHeader("Accept", "application/vnd.blackducksoftware.user-4+json")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String bearerToken = response.header("Authorization");
                    if (bearerToken != null) {
                        this.authToken = bearerToken;
                        logger.info("Successfully authenticated with Black Duck Hub");
                        return true;
                    }
                }
                logger.error("Authentication failed: {}", response.message());
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to authenticate with Black Duck Hub", e);
            return false;
        }
    }

    /**
     * Start a scan for the project
     */
    public String startScan(File projectDirectory) {
        if (!authenticate()) {
            throw new RuntimeException("Failed to authenticate with Black Duck Hub");
        }

        try {
            // Create or get project
            String projectId = createOrGetProject();
            if (projectId == null) {
                throw new RuntimeException("Failed to create or get project");
            }

            // Start scan
            return startScanForProject(projectId, projectDirectory);
        } catch (Exception e) {
            logger.error("Failed to start Black Duck scan", e);
            throw new RuntimeException("Failed to start Black Duck scan", e);
        }
    }

    private String createOrGetProject() throws IOException {
        // First try to get existing project
        Request request = new Request.Builder()
                .url(config.getBlackDuckUrl() + "/api/projects?q=name:" + config.getProjectName())
                .get()
                .addHeader("Authorization", authToken)
                .addHeader("Accept", "application/vnd.blackducksoftware.project-detail-4+json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode projects = objectMapper.readTree(response.body().string());
                if (projects.has("items") && projects.get("items").size() > 0) {
                    String projectId = projects.get("items").get(0).get("_meta").get("href").asText();
                    logger.info("Found existing project: {}", projectId);
                    return extractIdFromHref(projectId);
                }
            }
        }

        // Create new project if not found
        return createProject();
    }

    private String createProject() throws IOException {
        String projectJson = String.format("""
            {
                "name": "%s",
                "description": "Auto-created by Black Duck Maven Plugin",
                "projectTier": 1,
                "projectLevelAdjustments": true,
                "cloneCategories": [],
                "customSignatureEnabled": false
            }
            """, config.getProjectName());

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), projectJson);

        Request request = new Request.Builder()
                .url(config.getBlackDuckUrl() + "/api/projects")
                .post(body)
                .addHeader("Authorization", authToken)
                .addHeader("Accept", "application/vnd.blackducksoftware.project-detail-4+json")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String location = response.header("Location");
                if (location != null) {
                    logger.info("Created new project: {}", location);
                    return extractIdFromHref(location);
                }
            }
            logger.error("Failed to create project: {}", response.message());
            return null;
        }
    }

    private String startScanForProject(String projectId, File projectDirectory) throws IOException {
        // This is a simplified implementation
        // In a real implementation, you would use the Black Duck CLI or SDK
        logger.info("Starting scan for project {} in directory {}", projectId, projectDirectory.getAbsolutePath());
        
        // For now, return a mock scan ID
        return "scan-" + System.currentTimeMillis();
    }

    /**
     * Get scan results and vulnerabilities
     */
    public List<Vulnerability> getScanResults(String scanId) {
        // This would typically query the Black Duck API for scan results
        // For now, return mock vulnerabilities for demonstration
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        
        // Mock vulnerability data
        vulnerabilities.add(new Vulnerability(
            "CVE-2023-1234",
            "HIGH",
            "org.hibernate:hibernate-core:4.3.5.Final",
            "5.6.15.Final",
            "SQL Injection vulnerability in Hibernate Core"
        ));
        
        vulnerabilities.add(new Vulnerability(
            "CVE-2023-5678",
            "MEDIUM",
            "mysql:mysql-connector-java:5.0.5",
            "8.0.33",
            "Authentication bypass in MySQL Connector"
        ));
        
        return vulnerabilities;
    }

    private String extractIdFromHref(String href) {
        return href.substring(href.lastIndexOf('/') + 1);
    }

    /**
     * Represents a security vulnerability found by Black Duck
     */
    public static class Vulnerability {
        private final String cveId;
        private final String severity;
        private final String component;
        private final String fixedVersion;
        private final String description;

        public Vulnerability(String cveId, String severity, String component, String fixedVersion, String description) {
            this.cveId = cveId;
            this.severity = severity;
            this.component = component;
            this.fixedVersion = fixedVersion;
            this.description = description;
        }

        public String getCveId() { return cveId; }
        public String getSeverity() { return severity; }
        public String getComponent() { return component; }
        public String getFixedVersion() { return fixedVersion; }
        public String getDescription() { return description; }
        
        public String getGroupId() {
            return component.split(":")[0];
        }
        
        public String getArtifactId() {
            return component.split(":")[1];
        }
        
        public String getCurrentVersion() {
            return component.split(":")[2];
        }
    }
}