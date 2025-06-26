package com.rt.blackduck.util;

import com.rt.blackduck.config.PluginConfiguration;
import com.rt.blackduck.service.BlackDuckService.Vulnerability;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for manipulating POM files to fix security vulnerabilities
 */
public class PomManipulator {
    
    private static final Logger logger = LoggerFactory.getLogger(PomManipulator.class);
    
    private final PluginConfiguration.AutoFixConfiguration config;
    
    public PomManipulator(PluginConfiguration.AutoFixConfiguration config) {
        this.config = config;
    }

    /**
     * Apply fixes to the POM file based on vulnerabilities found
     */
    public boolean applyFixes(File pomFile, List<Vulnerability> vulnerabilities) {
        if (!config.isEnabled()) {
            logger.info("Auto-fix is disabled, skipping POM modifications");
            return false;
        }

        try {
            // Backup original POM if configured
            if (config.isBackupPom()) {
                backupPom(pomFile);
            }

            // Parse the POM file
            SAXReader reader = new SAXReader();
            Document document = reader.read(pomFile);
            Element root = document.getRootElement();

            boolean modified = false;

            // Process each vulnerability
            for (Vulnerability vulnerability : vulnerabilities) {
                logger.info("Processing vulnerability: {} for component: {}", 
                    vulnerability.getCveId(), vulnerability.getComponent());

                if (updateDirectDependency(root, vulnerability)) {
                    modified = true;
                } else if (addExclusionAndDirectDependency(root, vulnerability)) {
                    modified = true;
                }
            }

            // Write the modified POM back to file
            if (modified) {
                writePomFile(document, pomFile);
                logger.info("Successfully updated POM file with security fixes");
                return true;
            } else {
                logger.info("No modifications were needed for the POM file");
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to apply fixes to POM file", e);
            return false;
        }
    }

    private void backupPom(File pomFile) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File backupFile = new File(pomFile.getParent(), "pom.xml.backup." + timestamp);
        Files.copy(pomFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Created backup of POM file: {}", backupFile.getName());
    }

    private boolean updateDirectDependency(Element root, Vulnerability vulnerability) {
        if (!config.isUpdateDirectDependencies()) {
            return false;
        }

        // Find the dependencies element
        Element dependencies = root.element("dependencies");
        if (dependencies == null) {
            return false;
        }

        // Look for the direct dependency
        for (Element dependency : dependencies.elements("dependency")) {
            Element groupId = dependency.element("groupId");
            Element artifactId = dependency.element("artifactId");
            Element version = dependency.element("version");

            if (groupId != null && artifactId != null && version != null) {
                if (vulnerability.getGroupId().equals(groupId.getTextTrim()) &&
                    vulnerability.getArtifactId().equals(artifactId.getTextTrim()) &&
                    vulnerability.getCurrentVersion().equals(version.getTextTrim())) {
                    
                    // Check if the version update is allowed
                    if (isVersionUpdateAllowed(vulnerability.getCurrentVersion(), vulnerability.getFixedVersion())) {
                        version.setText(vulnerability.getFixedVersion());
                        logger.info("Updated direct dependency {}:{} from {} to {}", 
                            vulnerability.getGroupId(), vulnerability.getArtifactId(),
                            vulnerability.getCurrentVersion(), vulnerability.getFixedVersion());
                        return true;
                    } else {
                        logger.warn("Version update from {} to {} exceeds maximum allowed jump", 
                            vulnerability.getCurrentVersion(), vulnerability.getFixedVersion());
                    }
                }
            }
        }

        return false;
    }

    private boolean addExclusionAndDirectDependency(Element root, Vulnerability vulnerability) {
        if (!config.isAddExclusions() || !config.isAddDirectReplacements()) {
            return false;
        }

        // Find dependencies that might have this as a transitive dependency
        Element dependencies = root.element("dependencies");
        if (dependencies == null) {
            dependencies = root.addElement("dependencies");
        }

        boolean addedExclusion = false;
        boolean addedDirectDependency = false;

        // Add exclusions to existing dependencies that might pull in the vulnerable component
        for (Element dependency : dependencies.elements("dependency")) {
            if (addExclusionToDependency(dependency, vulnerability)) {
                addedExclusion = true;
            }
        }

        // Add the fixed version as a direct dependency
        if (addedExclusion || shouldAddDirectDependency(dependencies, vulnerability)) {
            addDirectDependency(dependencies, vulnerability);
            addedDirectDependency = true;
        }

        return addedExclusion || addedDirectDependency;
    }

    private boolean addExclusionToDependency(Element dependency, Vulnerability vulnerability) {
        // This is a simplified approach - in reality, you'd need to analyze the dependency tree
        // to determine which dependencies actually pull in the vulnerable component
        
        Element groupId = dependency.element("groupId");
        if (groupId != null && isLikelyToHaveTransitiveDependency(groupId.getTextTrim(), vulnerability)) {
            Element exclusions = dependency.element("exclusions");
            if (exclusions == null) {
                exclusions = dependency.addElement("exclusions");
            }

            // Check if exclusion already exists
            for (Element exclusion : exclusions.elements("exclusion")) {
                Element exGroupId = exclusion.element("groupId");
                Element exArtifactId = exclusion.element("artifactId");
                if (exGroupId != null && exArtifactId != null &&
                    vulnerability.getGroupId().equals(exGroupId.getTextTrim()) &&
                    vulnerability.getArtifactId().equals(exArtifactId.getTextTrim())) {
                    return false; // Exclusion already exists
                }
            }

            // Add new exclusion
            Element exclusion = exclusions.addElement("exclusion");
            exclusion.addElement("groupId").setText(vulnerability.getGroupId());
            exclusion.addElement("artifactId").setText(vulnerability.getArtifactId());

            logger.info("Added exclusion for {}:{} to dependency {}:{}", 
                vulnerability.getGroupId(), vulnerability.getArtifactId(),
                groupId.getTextTrim(), dependency.element("artifactId").getTextTrim());
            return true;
        }

        return false;
    }

    private boolean isLikelyToHaveTransitiveDependency(String dependencyGroupId, Vulnerability vulnerability) {
        // Simplified heuristic - in reality, you'd analyze the actual dependency tree
        return dependencyGroupId.startsWith("org.hibernate") && vulnerability.getGroupId().startsWith("org.hibernate") ||
               dependencyGroupId.startsWith("org.springframework") && vulnerability.getGroupId().startsWith("org.springframework") ||
               dependencyGroupId.equals("mysql") && vulnerability.getGroupId().equals("mysql");
    }

    private boolean shouldAddDirectDependency(Element dependencies, Vulnerability vulnerability) {
        // Check if the direct dependency already exists
        for (Element dependency : dependencies.elements("dependency")) {
            Element groupId = dependency.element("groupId");
            Element artifactId = dependency.element("artifactId");
            if (groupId != null && artifactId != null &&
                vulnerability.getGroupId().equals(groupId.getTextTrim()) &&
                vulnerability.getArtifactId().equals(artifactId.getTextTrim())) {
                return false; // Direct dependency already exists
            }
        }
        return true;
    }

    private void addDirectDependency(Element dependencies, Vulnerability vulnerability) {
        Element dependency = dependencies.addElement("dependency");
        dependency.addElement("groupId").setText(vulnerability.getGroupId());
        dependency.addElement("artifactId").setText(vulnerability.getArtifactId());
        dependency.addElement("version").setText(vulnerability.getFixedVersion());

        // Add comment explaining why this dependency was added
        dependency.addComment(String.format(" Added by Black Duck Auto-Fix to resolve %s ", vulnerability.getCveId()));

        logger.info("Added direct dependency {}:{}:{} to fix {}", 
            vulnerability.getGroupId(), vulnerability.getArtifactId(), 
            vulnerability.getFixedVersion(), vulnerability.getCveId());
    }

    private boolean isVersionUpdateAllowed(String currentVersion, String newVersion) {
        if (!config.isExcludeSnapshotVersions() && newVersion.contains("SNAPSHOT")) {
            return false;
        }

        // Simplified version comparison - in reality, you'd use a proper version comparison library
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");

            if (currentParts.length >= 2 && newParts.length >= 2) {
                int currentMajor = Integer.parseInt(currentParts[0]);
                int currentMinor = Integer.parseInt(currentParts[1]);
                int newMajor = Integer.parseInt(newParts[0]);
                int newMinor = Integer.parseInt(newParts[1]);

                switch (config.getMaxVersionJump().toUpperCase()) {
                    case "PATCH":
                        return newMajor == currentMajor && newMinor == currentMinor;
                    case "MINOR":
                        return newMajor == currentMajor;
                    case "MAJOR":
                        return true;
                    default:
                        return newMajor == currentMajor;
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse version numbers: {} -> {}", currentVersion, newVersion);
        }

        return true; // Allow update if we can't parse versions
    }

    private void writePomFile(Document document, File pomFile) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndentSize(4);
        format.setNewlines(true);
        format.setTrimText(false);

        try (FileWriter writer = new FileWriter(pomFile)) {
            XMLWriter xmlWriter = new XMLWriter(writer, format);
            xmlWriter.write(document);
            xmlWriter.close();
        }
    }
}