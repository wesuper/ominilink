package org.wesuper.liteai.bridge.javaseeker.config;

import java.util.List;
import java.util.ArrayList;

/**
 * A wrapper class that holds a list of {@link ProjectEntry} objects.
 * This class directly maps to the root structure of the {@code javaseeker-projects.yml} file,
 * typically under a "projects" key.
 */
public class ProjectConfigs {
    private List<ProjectEntry> projects = new ArrayList<>();

    /**
     * Gets the list of project entries.
     * @return A list of {@link ProjectEntry} objects; may be empty but not null.
     */
    public List<ProjectEntry> getProjects() {
        return projects;
    }

    /**
     * Sets the list of project entries.
     * @param projects A list of {@link ProjectEntry} objects.
     */
    public void setProjects(List<ProjectEntry> projects) {
        this.projects = projects;
    }
}
