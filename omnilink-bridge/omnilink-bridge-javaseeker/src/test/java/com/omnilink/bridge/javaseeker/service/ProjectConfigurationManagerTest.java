package com.omnilink.bridge.javaseeker.service;

import com.omnilink.bridge.javaseeker.config.ProjectEntry;
import com.omnilink.bridge.javaseeker.config.ProjectConfigs; // Added import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths; // Added import
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectConfigurationManagerTest {

    private ProjectConfigurationManager manager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new ProjectConfigurationManager();
    }

    private void initializeManagerWithConfigFile(String configContent) throws IOException {
        Path configFile = tempDir.resolve("test-javaseeker-projects.yml");
        Files.writeString(configFile, configContent);
        ReflectionTestUtils.setField(manager, "configFileLocation", configFile.toString());
        manager.init(); // Manually trigger init
    }

    @Test
    void testLoadConfigurations_Success() throws IOException {
        String yamlContent = "projects:\n" +
                             "  - name: \"test-project-git\"\n" + // Quoted names
                             "    sourceType: \"git\"\n" +
                             "    location: \"https://example.com/repo.git\"\n" +
                             "    branch: \"develop\"\n" +
                             "    localCachePath: \".cache/test-project-git\"\n" +
                             "  - name: \"test-project-local\"\n" +
                             "    sourceType: \"local\"\n" +
                             "    location: \"/tmp/local-repo\"\n" +
                             "    localCachePath: \"/tmp/local-repo-cache\"\n";
        initializeManagerWithConfigFile(yamlContent);

        List<ProjectEntry> projects = manager.getAllProjects();
        assertEquals(2, projects.size());

        Optional<ProjectEntry> gitProjectOpt = manager.getProject("test-project-git");
        assertTrue(gitProjectOpt.isPresent());
        ProjectEntry gitProject = gitProjectOpt.get();
        assertEquals("test-project-git", gitProject.getName());
        assertEquals("git", gitProject.getSourceType());
        assertTrue(Paths.get(gitProject.getLocalCachePath()).isAbsolute(), "Git project cache path should be absolute");
        assertTrue(gitProject.getLocalCachePath().endsWith(File.separator + ".cache" + File.separator + "test-project-git"), "Git project cache path incorrect");


        Optional<ProjectEntry> localProjectOpt = manager.getProject("test-project-local");
        assertTrue(localProjectOpt.isPresent());
        ProjectEntry localProject = localProjectOpt.get();
        assertEquals("test-project-local", localProject.getName());
        assertEquals("local", localProject.getSourceType());
        assertEquals("/tmp/local-repo-cache", localProject.getLocalCachePath());
        assertEquals("NOT_SYNCED", localProject.getStatus());
    }

    @Test
    void testLoadConfigurations_EmptyFile() throws IOException {
        initializeManagerWithConfigFile("projects:");
        assertTrue(manager.getAllProjects().isEmpty());
    }

    @Test
    void testDynamicReload() throws IOException, InterruptedException {
        Path configFile = tempDir.resolve("reload-test.yml");
        String initialContent = "projects:\n  - name: \"initial\"\n    sourceType: \"local\"\n    location: \"/data/initial\"";
        Files.writeString(configFile, initialContent);

        ReflectionTestUtils.setField(manager, "configFileLocation", configFile.toString());
        // ReflectionTestUtils.setField(manager, "lastModifiedTime", 0L); // Not needed, init sets it.
        manager.init();

        assertEquals(1, manager.getAllProjects().size());
        assertEquals("initial", manager.getProject("initial").get().getName());

        Thread.sleep(100);
        String updatedContent = "projects:\n  - name: \"updated\"\n    sourceType: \"git\"\n    location: \"https://new.com/repo.git\"";
        Files.writeString(configFile, updatedContent);

        // Ensure the lastModifiedTime in the manager is older than the file's new modification time.
        // This is crucial for the checkForUpdates to detect a change.
        ReflectionTestUtils.setField(manager, "lastModifiedTime", Files.getLastModifiedTime(configFile).toMillis() - 5000L);


        manager.checkForUpdates();

        assertEquals(1, manager.getAllProjects().size());
        assertTrue(manager.getProject("initial").isEmpty());
        assertTrue(manager.getProject("updated").isPresent());
        assertEquals("updated", manager.getProject("updated").get().getName());
    }

    @Test
    void testUpdateProjectStatus() throws IOException {
        String yamlContent = "projects:\n  - name: \"status-test\"\n    sourceType: \"local\"\n    location: \"/data/status\"";
        initializeManagerWithConfigFile(yamlContent);

        manager.updateProjectStatus("status-test", "READY");
        Optional<ProjectEntry> entry = manager.getProject("status-test");
        assertTrue(entry.isPresent());
        assertEquals("READY", entry.get().getStatus());

        manager.updateProjectStatus("non-existent", "READY");
    }

    @Test
    void testInit_DefaultConfigCreation() throws IOException {
        Path nonExistentConfig = tempDir.resolve("this-does-not-exist.yml");
        ReflectionTestUtils.setField(manager, "configFileLocation", nonExistentConfig.toString());
        manager.init();

        assertTrue(Files.exists(nonExistentConfig));
        ProjectConfigs configs = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
            .readValue(nonExistentConfig.toFile(), ProjectConfigs.class);
        assertNotNull(configs.getProjects(), "Projects list should not be null even if empty");
        assertTrue(configs.getProjects().isEmpty());
    }
}
