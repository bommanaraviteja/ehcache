package com.rt.blackduck.util;

import com.rt.blackduck.config.PluginConfiguration;
import com.rt.blackduck.service.BlackDuckService.Vulnerability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PomManipulatorTest {

    @TempDir
    File tempDir;

    private PomManipulator pomManipulator;
    private File pomFile;

    @BeforeEach
    void setUp() throws IOException {
        PluginConfiguration.AutoFixConfiguration config = new PluginConfiguration.AutoFixConfiguration();
        config.setEnabled(true);
        config.setUpdateDirectDependencies(true);
        config.setAddExclusions(true);
        config.setAddDirectReplacements(true);
        config.setBackupPom(true);
        config.setMaxVersionJump("MINOR");

        pomManipulator = new PomManipulator(config);
        pomFile = new File(tempDir, "pom.xml");
        createTestPom();
    }

    private void createTestPom() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.test</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencies>
                        <dependency>
                            <groupId>org.hibernate</groupId>
                            <artifactId>hibernate-core</artifactId>
                            <version>4.3.5.Final</version>
                        </dependency>
                        <dependency>
                            <groupId>mysql</groupId>
                            <artifactId>mysql-connector-java</artifactId>
                            <version>5.0.5</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        try (FileWriter writer = new FileWriter(pomFile)) {
            writer.write(pomContent);
        }
    }

    @Test
    void testApplyFixesWithDirectDependencyUpdate() throws IOException {
        // Use a version that allows MINOR updates
        List<Vulnerability> vulnerabilities = Arrays.asList(
                new Vulnerability(
                        "CVE-2023-1234",
                        "HIGH",
                        "org.hibernate:hibernate-core:4.3.5.Final",
                        "4.4.0.Final",
                        "SQL Injection vulnerability"
                )
        );

        boolean result = pomManipulator.applyFixes(pomFile, vulnerabilities);

        assertTrue(result);
        String pomContent = Files.readString(pomFile.toPath());
        assertTrue(pomContent.contains("4.4.0.Final"));
        assertFalse(pomContent.contains("4.3.5.Final"));
    }

    @Test
    void testApplyFixesCreatesBackup() throws IOException {
        List<Vulnerability> vulnerabilities = Arrays.asList(
                new Vulnerability(
                        "CVE-2023-1234",
                        "HIGH",
                        "org.hibernate:hibernate-core:4.3.5.Final",
                        "5.6.15.Final",
                        "SQL Injection vulnerability"
                )
        );

        pomManipulator.applyFixes(pomFile, vulnerabilities);

        // Check that backup file was created
        File[] backupFiles = tempDir.listFiles((dir, name) -> name.startsWith("pom.xml.backup."));
        assertNotNull(backupFiles);
        assertEquals(1, backupFiles.length);
    }

    @Test
    void testApplyFixesWithNoVulnerabilities() {
        List<Vulnerability> vulnerabilities = Arrays.asList();

        boolean result = pomManipulator.applyFixes(pomFile, vulnerabilities);

        assertFalse(result);
    }

    @Test
    void testApplyFixesWithDisabledAutoFix() {
        PluginConfiguration.AutoFixConfiguration config = new PluginConfiguration.AutoFixConfiguration();
        config.setEnabled(false);
        PomManipulator disabledManipulator = new PomManipulator(config);

        List<Vulnerability> vulnerabilities = Arrays.asList(
                new Vulnerability(
                        "CVE-2023-1234",
                        "HIGH",
                        "org.hibernate:hibernate-core:4.3.5.Final",
                        "5.6.15.Final",
                        "SQL Injection vulnerability"
                )
        );

        boolean result = disabledManipulator.applyFixes(pomFile, vulnerabilities);

        assertFalse(result);
    }
}