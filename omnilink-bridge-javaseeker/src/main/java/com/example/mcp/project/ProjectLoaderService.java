package com.example.mcp.project;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

@Service
public class ProjectLoaderService {
    private final Map<String, ProjectSource> activeProjects = new HashMap<>();

    public Optional<ProjectSource> loadProject(String projectName, String sourceIdentifier, String branchName) {
        ProjectSource projectSource;
        if (sourceIdentifier.startsWith("http://") || sourceIdentifier.startsWith("https://") || sourceIdentifier.startsWith("git@")) {
            projectSource = new GitProjectSource(projectName, sourceIdentifier, branchName);
        } else {
            projectSource = new LocalProjectSource(projectName, sourceIdentifier);
        }

        if (projectSource.resolve()) {
            activeProjects.put(projectName, projectSource);
            return Optional.of(projectSource);
        }
        return Optional.empty();
    }

    public Optional<ProjectSource> getProject(String projectName) {
        return Optional.ofNullable(activeProjects.get(projectName));
    }

    // Optional: Method to unload/cleanup a project if needed
    public void unloadProject(String projectName) {
        ProjectSource projectSource = activeProjects.remove(projectName);
        if (projectSource instanceof GitProjectSource) {
            // Implement cleanup logic for Git projects (e.g., delete temp directory)
            try {
                Path projectPath = projectSource.getProjectPath();
                if (projectPath != null && Files.exists(projectPath)) {
                    Files.walk(projectPath)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
                    System.out.println("Cleaned up project: " + projectName + " at " + projectPath);
                }
            } catch (IOException e) {
                System.err.println("Error cleaning up project " + projectName + ": " + e.getMessage());
            }
        }
    }
}
