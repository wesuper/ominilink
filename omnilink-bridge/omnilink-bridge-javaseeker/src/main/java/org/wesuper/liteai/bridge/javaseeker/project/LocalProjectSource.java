package org.wesuper.liteai.bridge.javaseeker.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalProjectSource implements ProjectSource {
    private final String projectName;
    private final Path projectPath;

    public LocalProjectSource(String projectName, String path) {
        this.projectName = projectName;
        this.projectPath = Paths.get(path);
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
        // For local projects, just check if the path exists and is a directory
        return Files.exists(projectPath) && Files.isDirectory(projectPath);
    }
}
