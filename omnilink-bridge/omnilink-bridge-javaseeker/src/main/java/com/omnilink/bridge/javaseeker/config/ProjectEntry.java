package com.omnilink.bridge.javaseeker.config;

import java.util.Objects;

/**
 * Represents a single Java project configuration entry defined in the
 * {@code javaseeker-projects.yml} file.
 * <p>
 * Each entry details how a project should be sourced, cached, and its current status
 * within the JavaSeeker analysis lifecycle.
 * </p>
 */
public class ProjectEntry {
    private String name;
    private String sourceType; // "git" or "local"
    private String location;   // URL for git, path for local
    private String branch;     // Optional, for git
    private String localCachePath; // Path to store/access the project locally
    private String status;     // e.g., "NOT_SYNCED", "SYNCING", "COMPILING", "READY", "FAILED"

    /**
     * Default constructor. Initializes status to "NOT_SYNCED".
     */
    public ProjectEntry() {
        this.status = "NOT_SYNCED"; // Default status
    }

    /**
     * Gets the unique name of the project.
     * @return The project name.
     */
    public String getName() { return name; }
    /**
     * Sets the unique name of the project.
     * @param name The project name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the source type of the project (e.g., "git", "local").
     * @return The source type.
     */
    public String getSourceType() { return sourceType; }
    /**
     * Sets the source type of the project.
     * @param sourceType The source type ("git" or "local").
     */
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    /**
     * Gets the location of the project source.
     * For "git" type, this is the repository URL.
     * For "local" type, this is the file system path.
     * @return The project location string.
     */
    public String getLocation() { return location; }
    /**
     * Sets the location of the project source.
     * @param location The repository URL or file system path.
     */
    public void setLocation(String location) { this.location = location; }

    /**
     * Gets the Git branch to be used if sourceType is "git".
     * @return The Git branch name, or null if not applicable/default.
     */
    public String getBranch() { return branch; }
    /**
     * Sets the Git branch.
     * @param branch The Git branch name.
     */
    public void setBranch(String branch) { this.branch = branch; }

    /**
     * Gets the local file system path where the project source code is cached or located.
     * @return The absolute path to the local cache.
     */
    public String getLocalCachePath() { return localCachePath; }
    /**
     * Sets the local file system path for caching/accessing the project.
     * @param localCachePath The path string.
     */
    public void setLocalCachePath(String localCachePath) { this.localCachePath = localCachePath; }

    /**
     * Gets the current lifecycle status of the project.
     * (e.g., "NOT_SYNCED", "SYNCING", "COMPILING", "READY", "FAILED").
     * @return The project status string.
     */
    public String getStatus() { return status; }
    /**
     * Sets the current lifecycle status of the project.
     * @param status The project status string.
     */
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectEntry that = (ProjectEntry) o;
        // Status is mutable and not part of identity for equality
        return Objects.equals(name, that.name) &&
               Objects.equals(sourceType, that.sourceType) &&
               Objects.equals(location, that.location) &&
               Objects.equals(branch, that.branch) &&
               Objects.equals(localCachePath, that.localCachePath);
    }

    @Override
    public int hashCode() {
        // Status is mutable and not part of identity for hashcode
        return Objects.hash(name, sourceType, location, branch, localCachePath);
    }

    @Override
    public String toString() {
        return "ProjectEntry{" +
               "name='" + name + '\'' +
               ", sourceType='" + sourceType + '\'' +
               ", location='" + location + '\'' +
               ", branch='" + branch + '\'' +
               ", localCachePath='" + localCachePath + '\'' +
               ", status='" + status + '\'' +
               '}';
    }
}
