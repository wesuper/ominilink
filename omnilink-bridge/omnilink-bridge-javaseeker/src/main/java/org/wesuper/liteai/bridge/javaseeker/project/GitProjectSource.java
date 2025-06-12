package org.wesuper.liteai.bridge.javaseeker.project;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public class GitProjectSource implements ProjectSource {
    private final String projectName;
    private final String repositoryUrl;
    private final String branch;
    private Path projectPath; // Will be set after cloning

    public GitProjectSource(String projectName, String repositoryUrl, String branch) {
        this.projectName = projectName;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public Path getProjectPath() {
        return projectPath;
    }

    @Override
    public boolean resolve() {
        try {
            // Create a temporary directory to clone the repository
            this.projectPath = Files.createTempDirectory("mcp_git_project_" + projectName + "_");
            System.out.println("Cloning " + repositoryUrl + (branch != null ? " (branch: " + branch + ")" : "") + " into " + projectPath);

            Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(projectPath.toFile())
                .setBranch(branch != null ? branch : "main") // Default to 'main' if no branch specified
                .call();

            System.out.println("Cloned successfully.");
            return true;
        } catch (IOException | GitAPIException e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            e.printStackTrace();
            // Cleanup if cloning failed
            if (projectPath != null) {
                try {
                    // A simple recursive delete for the temp directory
                    Files.walk(projectPath)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
                } catch (IOException ioe) {
                    System.err.println("Error cleaning up temp directory: " + ioe.getMessage());
                }
            }
            return false;
        }
    }
}
