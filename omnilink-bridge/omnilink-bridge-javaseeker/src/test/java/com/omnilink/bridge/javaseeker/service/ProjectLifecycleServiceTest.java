package com.omnilink.bridge.javaseeker.service;

import com.omnilink.bridge.javaseeker.config.ProjectEntry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository; // Added for mocking getRepository().getBranch()

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List; // Added for mocking branchList()
import org.eclipse.jgit.lib.Ref; // Added for mocking branchList()

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future; // Keep this for mocking submit

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectLifecycleServiceTest {

    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private ExecutorService mockExecutorService;

    @InjectMocks
    private ProjectLifecycleService projectLifecycleService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
         Mockito.lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            @SuppressWarnings("unchecked") // Mocking generic Future
            Future<Void> mockFuture = mock(Future.class);
            return mockFuture;
        }).when(mockExecutorService).submit(any(Runnable.class));
    }

    @Test
    void testProcessProjects_Git_CloneAndCompileSuccess() throws Exception {
        ProjectEntry gitProject = new ProjectEntry();
        gitProject.setName("git-test");
        gitProject.setSourceType("git");
        gitProject.setLocation("https://example.com/repo.git");
        gitProject.setBranch("main");
        Path cachePath = tempDir.resolve("git-test-cache");
        gitProject.setLocalCachePath(cachePath.toString());
        gitProject.setStatus("NOT_SYNCED");

        when(projectConfigurationManager.getAllProjects()).thenReturn(Collections.singletonList(gitProject));

        try (MockedStatic<Git> mockedGit = Mockito.mockStatic(Git.class)) {
            CloneCommand mockCloneCommand = mock(CloneCommand.class);
            mockedGit.when(() -> Git.cloneRepository()).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setBranch(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setBare(anyBoolean())).thenReturn(mockCloneCommand);
            Git mockGitClonedInstance = mock(Git.class);
            when(mockCloneCommand.call()).thenReturn(mockGitClonedInstance);

            Files.createDirectories(cachePath);
            Files.createFile(cachePath.resolve("pom.xml"));

            projectLifecycleService.processProjects();

            verify(projectConfigurationManager).updateProjectStatus("git-test", "SYNCING");
            verify(mockCloneCommand).call();
            verify(projectConfigurationManager).updateProjectStatus("git-test", "COMPILING");
            verify(projectConfigurationManager).updateProjectStatus("git-test", "READY");
        }
    }

    @Test
    void testProcessProjects_Git_PullAndCompile() throws Exception {
        ProjectEntry gitProject = new ProjectEntry();
        gitProject.setName("git-pull-test");
        gitProject.setSourceType("git");
        gitProject.setLocation("https://example.com/another.git");
        gitProject.setBranch("main"); // Explicitly set branch
        Path cachePath = tempDir.resolve("git-pull-cache");
        Files.createDirectories(cachePath);
        Files.createDirectory(cachePath.resolve(".git"));
        gitProject.setLocalCachePath(cachePath.toString());
        gitProject.setStatus("NOT_SYNCED");

        when(projectConfigurationManager.getAllProjects()).thenReturn(Collections.singletonList(gitProject));

        try (MockedStatic<Git> mockedGit = Mockito.mockStatic(Git.class)) {
            Git mockGitInstance = mock(Git.class);
            PullCommand mockPullCommand = mock(PullCommand.class);
            PullResult mockPullResult = mock(PullResult.class);
            Status mockStatus = mock(Status.class);
            Repository mockRepository = mock(Repository.class); // For getRepository().getBranch()
            Ref mockBranchRef = mock(Ref.class); // For branchList()
            List<Ref> mockBranchList = Collections.singletonList(mockBranchRef);


            mockedGit.when(() -> Git.open(any(File.class))).thenReturn(mockGitInstance);
            when(mockGitInstance.getRepository()).thenReturn(mockRepository); // Mock getRepository
            when(mockRepository.getBranch()).thenReturn("main"); // Mock getBranch
            when(mockGitInstance.branchList()).thenReturn(mock(org.eclipse.jgit.api.ListBranchCommand.class)); // Mock branchList() to return a command object
            when(mockGitInstance.branchList().call()).thenReturn(mockBranchList); // Mock call() on command object
            when(mockBranchRef.getName()).thenReturn("refs/heads/main"); // Make local branch exist

            when(mockGitInstance.fetch()).thenReturn(mock(org.eclipse.jgit.api.FetchCommand.class)); // Mock fetch()
            when(mockGitInstance.fetch().setRemote(anyString())).thenReturn(mock(org.eclipse.jgit.api.FetchCommand.class));
            when(mockGitInstance.fetch().setRemote(anyString()).setRefSpecs(anyString())).thenReturn(mock(org.eclipse.jgit.api.FetchCommand.class));


            when(mockGitInstance.pull()).thenReturn(mockPullCommand);
            when(mockPullCommand.setRemote(anyString())).thenReturn(mockPullCommand); // For specific remote
            when(mockPullCommand.setRemoteBranchName(anyString())).thenReturn(mockPullCommand);
            when(mockPullCommand.call()).thenReturn(mockPullResult);
            when(mockPullResult.isSuccessful()).thenReturn(true);
            when(mockGitInstance.status()).thenReturn(mockStatus);
            when(mockStatus.getConflicting()).thenReturn(new HashSet<>());


            Files.createFile(cachePath.resolve("build.gradle"));

            projectLifecycleService.processProjects();

            verify(projectConfigurationManager).updateProjectStatus("git-pull-test", "SYNCING");
            verify(mockPullCommand).call();
            verify(projectConfigurationManager).updateProjectStatus("git-pull-test", "COMPILING");
            verify(projectConfigurationManager).updateProjectStatus("git-pull-test", "READY");
        }
    }

     @Test
    void testCompileProject_NoBuildFile() throws IOException {
        ProjectEntry project = new ProjectEntry();
        project.setName("no-build-file-project");
        project.setSourceType("local");
        Path cachePath = tempDir.resolve("no-build-file-cache");
        Files.createDirectories(cachePath);
        project.setLocalCachePath(cachePath.toString());
        project.setStatus("COMPILING");

        when(projectConfigurationManager.getAllProjects()).thenReturn(Collections.singletonList(project));
        projectLifecycleService.processProjects();
        verify(projectConfigurationManager).updateProjectStatus("no-build-file-project", "READY_NO_BUILD_FILE");
    }

}
