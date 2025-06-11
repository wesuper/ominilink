package com.omnilink.bridge.javaseeker.service;

import com.omnilink.bridge.javaseeker.config.ProjectEntry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Comparator; // For Files.walk delete


/**
 * Manages the lifecycle of Java projects defined in the configuration.
 * <p>
 * This service handles:
 * <ul>
 *     <li>Periodically checking projects based on their configuration from {@link ProjectConfigurationManager}.</li>
 *     <li>Synchronizing Git-based projects by cloning new repositories or pulling updates for existing ones
 *         into their specified {@code localCachePath}.</li>
 *     <li>Triggering compilation (e.g., {@code mvn compile} or {@code gradle build -x test}) for projects
 *         to resolve dependencies and prepare them for analysis.</li>
 *     <li>Updating the project's status (e.g., "SYNCING", "COMPILING", "READY", "FAILED") via
 *         {@link ProjectConfigurationManager}.</li>
 * </ul>
 * Operations are performed sequentially for each project using a single-threaded executor.
 * </p>
 */
@Service
public class ProjectLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectLifecycleService.class);
    private final ProjectConfigurationManager projectConfigurationManager;
    private final ExecutorService projectProcessingExecutor = Executors.newSingleThreadExecutor();

    /**
     * Constructs a new ProjectLifecycleService.
     * @param projectConfigurationManager The manager for accessing project configurations.
     */
    @Autowired
    public ProjectLifecycleService(ProjectConfigurationManager projectConfigurationManager) {
        this.projectConfigurationManager = projectConfigurationManager;
    }

    /**
     * Periodically processes all configured projects.
     * Iterates through projects obtained from {@link ProjectConfigurationManager} and submits
     * each for individual processing in a dedicated executor thread.
     */
    @Scheduled(fixedDelayString = "${javaseeker.lifecycle.check.delay:10000}", initialDelay = 7000) // Slightly after config manager init
    public void processProjects() {
        List<ProjectEntry> projects = projectConfigurationManager.getAllProjects();
        if (projects.isEmpty()) {
            logger.debug("No projects configured to process.");
            return;
        }
        logger.info("Checking lifecycle status for {} projects...", projects.size());
        for (ProjectEntry project : projects) {
            projectProcessingExecutor.submit(() -> {
                try {
                    processSingleProject(project);
                } catch (Exception e) {
                    logger.error("Unexpected error processing project '{}': {}", project.getName(), e.getMessage(), e);
                    projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_UNEXPECTED_ERROR");
                }
            });
        }
    }

    /**
     * Processes an individual project, handling its synchronization and compilation based on its
     * current status and configuration.
     * @param project The {@link ProjectEntry} to process.
     */
    private void processSingleProject(ProjectEntry project) {
        logger.debug("Processing project: {} (Current status: {})", project.getName(), project.getStatus());
        Path localPath = Paths.get(project.getLocalCachePath());

        if ("git".equalsIgnoreCase(project.getSourceType())) {
            if ("NOT_SYNCED".equalsIgnoreCase(project.getStatus()) ||
                "FAILED_SYNC".equalsIgnoreCase(project.getStatus()) ||
                "FAILED_MERGE_CONFLICT".equalsIgnoreCase(project.getStatus())) {
                syncGitProject(project, localPath);
            } else {
                logger.debug("Git project '{}' status ('{}') does not require sync.", project.getName(), project.getStatus());
            }
        } else if ("local".equalsIgnoreCase(project.getSourceType())) {
            if (!Files.exists(localPath) || !Files.isDirectory(localPath)) {
                logger.error("Local project '{}' path does not exist or is not a directory: {}", project.getName(), localPath);
                projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_INVALID_PATH");
                return; // Stop processing this project if path is invalid
            }
            // If local project is not ready or failed in a build step, try to compile
            if (!"READY".equalsIgnoreCase(project.getStatus()) &&
                !"READY_NO_BUILD_FILE".equalsIgnoreCase(project.getStatus()) &&
                !"COMPILING".equalsIgnoreCase(project.getStatus())) { // Avoid re-triggering compile if already compiling
                 projectConfigurationManager.updateProjectStatus(project.getName(), "COMPILING");
            } else {
                 logger.debug("Local project '{}' status ('{}') does not require initial compilation trigger.", project.getName(), project.getStatus());
            }
        }

        // Re-fetch project entry in case status was updated by syncGitProject or initial local check
        ProjectEntry currentProjectState = projectConfigurationManager.getProject(project.getName())
                                             .orElseThrow(() -> new IllegalStateException("Project vanished during processing: " + project.getName()));

        if ("COMPILING".equalsIgnoreCase(currentProjectState.getStatus())) {
            compileProject(currentProjectState, localPath);
        }

        logger.debug("Finished processing cycle for project: {}", project.getName());
    }

    /**
     * Synchronizes a Git-based project.
     * If the local cache path is empty or doesn't exist, it clones the repository.
     * Otherwise, it attempts to pull updates from the configured branch.
     * Updates project status to "SYNCING", then "COMPILING" on success, or "FAILED_SYNC" / "FAILED_MERGE_CONFLICT" on failure.
     * @param project The Git project entry.
     * @param localPath The local path for the Git repository cache.
     */
    private void syncGitProject(ProjectEntry project, Path localPath) {
        projectConfigurationManager.updateProjectStatus(project.getName(), "SYNCING");
        logger.info("Syncing Git project '{}' from {} (branch: {}) to {}", project.getName(), project.getLocation(), project.getBranch(), localPath);
        String targetBranch = project.getBranch() != null && !project.getBranch().trim().isEmpty() ? project.getBranch().trim() : "main";

        try {
            if (Files.exists(localPath) && Files.isDirectory(localPath) && Files.exists(localPath.resolve(".git"))) {
                logger.info("Local path {} exists, attempting to pull updates for branch '{}'.", localPath, targetBranch);
                Git git = Git.open(localPath.toFile());
                String currentBranch = git.getRepository().getBranch();

                if (!currentBranch.equals(targetBranch)) {
                    logger.info("Project '{}' current branch '{}' differs from target '{}'. Checking out target branch.",
                                project.getName(), currentBranch, targetBranch);
                    // TODO: Add more robust branch switching logic (e.g., check if branch exists locally/remotely)
                    git.checkout().setName(targetBranch).setForced(true).call();
                }

                PullResult pullResult = git.pull().setRemote("origin").setRemoteBranchName(targetBranch).call();
                if (!pullResult.isSuccessful()) {
                    logger.warn("Pull not entirely successful for project '{}'. Merge conflicts might exist or other issues.", project.getName());
                    if (git.status().call().getConflicting().size() > 0) {
                        logger.error("Merge conflicts detected for project '{}'. Manual resolution required. Path: {}", project.getName(), localPath);
                        projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_MERGE_CONFLICT");
                        return;
                    }
                } else {
                    logger.info("Successfully pulled updates for project '{}'", project.getName());
                }
            } else {
                logger.info("Local path {} does not exist or is not a valid git repo, cloning repository for branch '{}'.", localPath, targetBranch);
                if(Files.exists(localPath) && Files.isDirectory(localPath)){ // If exists but not a git repo or empty
                    logger.warn("Local path {} exists but is non-empty and not a .git repo. It will be deleted and overwritten by clone.", localPath);
                    Files.walk(localPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
                Files.createDirectories(localPath);
                Git.cloneRepository()
                   .setURI(project.getLocation())
                   .setBranch(targetBranch)
                   .setDirectory(localPath.toFile())
                   .setBare(false)
                   .call();
                logger.info("Successfully cloned project '{}'", project.getName());
            }
            projectConfigurationManager.updateProjectStatus(project.getName(), "COMPILING");
        } catch (GitAPIException | IOException e) {
            logger.error("Error syncing Git project '{}': {}", project.getName(), e.getMessage(), e);
            projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_SYNC");
        }
    }

    /**
     * Compiles a project located at the given path.
     * Detects if it's a Maven or Gradle project by checking for {@code pom.xml} or {@code build.gradle[.kts]}.
     * Executes the appropriate build command (using wrappers if available).
     * Updates project status to "COMPILING", then "READY" on success, or various "FAILED_*" statuses.
     * @param project The project entry.
     * @param projectPath The path to the project's source code.
     */
    private void compileProject(ProjectEntry project, Path projectPath) {
        logger.info("Compiling project '{}' at {}", project.getName(), projectPath);

        String buildCommand;
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        if (Files.exists(projectPath.resolve("pom.xml"))) {
            String mvnExecutable = Files.exists(projectPath.resolve(isWindows ? "mvnw.cmd" : "mvnw")) ?
                                   (isWindows ? "mvnw.cmd" : "./mvnw") : "mvn";
            buildCommand = mvnExecutable + " clean compile -DskipTests";
        } else if (Files.exists(projectPath.resolve("build.gradle")) || Files.exists(projectPath.resolve("build.gradle.kts"))) {
            String gradleExecutable = Files.exists(projectPath.resolve(isWindows ? "gradlew.bat" : "gradlew")) ?
                                      (isWindows ? "gradlew.bat" : "./gradlew") : "gradle";
            buildCommand = gradleExecutable + " build -x test --no-daemon";
        } else {
            logger.warn("No pom.xml or build.gradle[.kts] found for project '{}'. Cannot determine build command. Marking as READY (with warning).", project.getName());
            projectConfigurationManager.updateProjectStatus(project.getName(), "READY_NO_BUILD_FILE");
            return;
        }
        logger.info("Executing build command: '{}' in directory: {}", buildCommand, projectPath);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand.split("\\s+"));
            processBuilder.directory(projectPath.toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                    logger.trace("[{}] BUILD: {}", project.getName(), line); // Trace level for build logs
                }
            }

            boolean exited = process.waitFor(10, TimeUnit.MINUTES);
            if (!exited) {
                process.destroyForcibly(); // Ensure process is killed on timeout
                logger.error("Build command timed out for project '{}'", project.getName());
                projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_BUILD_TIMEOUT");
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                logger.info("Successfully compiled project '{}'", project.getName());
                projectConfigurationManager.updateProjectStatus(project.getName(), "READY");
            } else {
                // Log only a summary of the output to avoid flooding logs, full output can be very long.
                logger.error("Build failed for project '{}' with exit code {}. Output (first 2KB):\n{}",
                             project.getName(), exitCode, output.substring(0, Math.min(output.length(), 2048)));
                projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_BUILD");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error during compilation process for project '{}': {}", project.getName(), e.getMessage(), e);
            projectConfigurationManager.updateProjectStatus(project.getName(), "FAILED_BUILD_EXCEPTION");
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Shuts down the internal executor service.
     * This method should be called when the application is shutting down to ensure
     * graceful termination of ongoing project processing tasks.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ProjectLifecycleService executor.");
        projectProcessingExecutor.shutdown();
        try {
            if (!projectProcessingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                projectProcessingExecutor.shutdownNow();
                if (!projectProcessingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                     logger.error("Project processing executor did not terminate.");
                }
            }
        } catch (InterruptedException e) {
            projectProcessingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("Shutdown interrupted for project processing executor.", e);
        }
         logger.info("ProjectLifecycleService executor shutdown complete.");
    }
}
