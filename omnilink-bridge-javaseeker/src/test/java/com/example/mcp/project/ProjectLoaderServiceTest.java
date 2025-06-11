package com.example.mcp.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProjectLoaderServiceTest {

    private ProjectLoaderService projectLoaderService;

    @TempDir
    Path tempDir; // Used for temporary local project paths

    @BeforeEach
    void setUp() {
        projectLoaderService = new ProjectLoaderService();
    }

    @Test
    void testLoadLocalProject_Success() throws IOException {
        Path projectPath = Files.createDirectory(tempDir.resolve("testProject"));
        String projectName = "localTest";

        try (MockedConstruction<LocalProjectSource> mocked = Mockito.mockConstruction(LocalProjectSource.class,
                (mock, context) -> {
                    when(mock.getProjectName()).thenReturn(projectName);
                    when(mock.getProjectPath()).thenReturn(projectPath);
                    when(mock.resolve()).thenReturn(true);
                })) {

            Optional<ProjectSource> result = projectLoaderService.loadProject(projectName, projectPath.toString(), null);

            assertTrue(result.isPresent());
            assertEquals(projectName, result.get().getProjectName());
            assertEquals(projectPath, result.get().getProjectPath());
            verify(mocked.constructed().get(0)).resolve();
        }
    }

    @Test
    void testLoadLocalProject_Failure_PathNotExists() {
         String projectName = "nonExistentLocal";
         String projectPath = tempDir.resolve("nonExistent").toString();

        try (MockedConstruction<LocalProjectSource> mocked = Mockito.mockConstruction(LocalProjectSource.class,
                (mock, context) -> {
                    when(mock.resolve()).thenReturn(false); // Simulate path not resolving
                })) {
           Optional<ProjectSource> result = projectLoaderService.loadProject(projectName, projectPath, null);
           assertFalse(result.isPresent());
        }
    }

    @Test
    void testLoadGitProject_Success() {
        String projectName = "gitTest";
        String repoUrl = "https://github.com/example/repo.git";
        String branch = "main";
        Path clonedPath = tempDir.resolve("clonedRepo");


        try (MockedConstruction<GitProjectSource> mocked = Mockito.mockConstruction(GitProjectSource.class,
                (mock, context) -> {
                    when(mock.getProjectName()).thenReturn(projectName);
                    when(mock.getProjectPath()).thenReturn(clonedPath); // Simulate path after cloning
                    when(mock.resolve()).thenReturn(true); // Simulate successful clone
                })) {

            Optional<ProjectSource> result = projectLoaderService.loadProject(projectName, repoUrl, branch);

            assertTrue(result.isPresent());
            assertEquals(projectName, result.get().getProjectName());
            assertEquals(clonedPath, result.get().getProjectPath());
            verify(mocked.constructed().get(0)).resolve();
        }
    }

    @Test
    void testLoadGitProject_Failure_CloningFails() {
        String projectName = "gitFailTest";
        String repoUrl = "https://github.com/example/fail.git";

       try (MockedConstruction<GitProjectSource> mocked = Mockito.mockConstruction(GitProjectSource.class,
                (mock, context) -> {
                    when(mock.resolve()).thenReturn(false); // Simulate failed clone
                })) {
           Optional<ProjectSource> result = projectLoaderService.loadProject(projectName, repoUrl, null);
           assertFalse(result.isPresent());
       }
    }


    @Test
    void testGetProject_Existing() throws IOException {
        Path projectPath = Files.createDirectory(tempDir.resolve("existingProject"));
        String projectName = "existing";

        try (MockedConstruction<LocalProjectSource> mocked = Mockito.mockConstruction(LocalProjectSource.class,
                (mock, context) -> {
                    when(mock.resolve()).thenReturn(true);
                })) {
           projectLoaderService.loadProject(projectName, projectPath.toString(), null);
        }

        Optional<ProjectSource> result = projectLoaderService.getProject(projectName);
        assertTrue(result.isPresent());
    }

    @Test
    void testGetProject_NonExisting() {
        Optional<ProjectSource> result = projectLoaderService.getProject("nonExistingProject");
        assertFalse(result.isPresent());
    }

    @Test
    void testUnloadProject_GitProject() {
        String projectName = "gitToUnload";
        String repoUrl = "https://github.com/example/unload.git";
        Path clonedPath = tempDir.resolve("gitToUnloadDir"); // Dummy path

        // Can't easily mock the static Files.walk for deletion verification in this setup
        // So we focus on the removal from activeProjects map
        try (MockedConstruction<GitProjectSource> mocked = Mockito.mockConstruction(GitProjectSource.class,
            (mock, context) -> {
                when(mock.getProjectName()).thenReturn(projectName);
                when(mock.getProjectPath()).thenReturn(clonedPath); // Important for unload
                when(mock.resolve()).thenReturn(true);
            })) {
            projectLoaderService.loadProject(projectName, repoUrl, null);
        }

        assertTrue(projectLoaderService.getProject(projectName).isPresent());
        projectLoaderService.unloadProject(projectName);
        assertFalse(projectLoaderService.getProject(projectName).isPresent());
    }
}
