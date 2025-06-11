package com.example.mcp.project;

import java.nio.file.Path;

public interface ProjectSource {
    String getProjectName();
    Path getProjectPath(); // Path to the root of the project
    boolean resolve(); // Method to prepare the project (e.g., clone if Git, verify path if local)
}
