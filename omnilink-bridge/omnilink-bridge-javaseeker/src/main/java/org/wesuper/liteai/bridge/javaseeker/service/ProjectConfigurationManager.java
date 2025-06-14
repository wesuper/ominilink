package org.wesuper.liteai.bridge.javaseeker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.wesuper.liteai.bridge.javaseeker.config.ProjectConfigs;
import org.wesuper.liteai.bridge.javaseeker.config.ProjectEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
// import org.springframework.context.ApplicationEventPublisher; // For more advanced change notification
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList; // Added for new ArrayList in getAllProjects
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * Manages the loading, parsing, and dynamic reloading of Java project configurations
 * from the {@code javaseeker-projects.yml} file.
 * <p>
 * This service is responsible for:
 * <ul>
 *     <li>Initializing project configurations from a YAML file at startup.
 *     The file can be an absolute path or a classpath resource. If it's a classpath
 *     resource, it's copied to an external location to allow for runtime modifications.</li>
 *     <li>Periodically monitoring the external YAML file for changes and reloading
 *     configurations if the file is modified.</li>
 *     <li>Providing access to the current list of {@link ProjectEntry} configurations.</li>
 *     <li>Updating the in-memory status of projects.</li>
 *     <li>Ensuring {@code localCachePath} for each project is an absolute path and the directory exists.</li>
 * </ul>
 * </p>
 */
@Service
// @EnableScheduling is on McpJavaSeekerApplication, so not strictly needed here if component scan picks this up.
// However, having it here doesn't hurt and makes the class's scheduling intent clearer.
public class ProjectConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectConfigurationManager.class);
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, ProjectEntry> projectConfigMap = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock(); // Lock for synchronizing access to config file and map

    // private final ApplicationEventPublisher eventPublisher; // Optional: for event-driven updates

    @Value("${javaseeker.config.file:javaseeker-projects.yml}")
    private String configFileLocation;

    private Path externalConfigPath;
    private long lastModifiedTime = 0L;

    /**
     * Initializes the ProjectConfigurationManager upon bean construction.
     * This method determines the effective location of the {@code javaseeker-projects.yml}
     * configuration file. If the path specified by {@code javaseeker.config.file}
     * is absolute and exists, it's used directly. Otherwise, it attempts to locate
     * the file in the classpath. If found in the classpath, it's copied to an
     * external location (relative to the application's working directory) to allow
     * for runtime modifications. If the file is not found in either location, a default
     * empty configuration file is created at the external location.
     * After establishing the configuration file path, an initial load of project
     * configurations is performed.
     */
    @PostConstruct
    public void init() {
        Path configPath = Paths.get(configFileLocation);
        if (configPath.isAbsolute() && Files.exists(configPath)) {
            externalConfigPath = configPath;
            logger.info("Using external configuration file: {}", externalConfigPath.toAbsolutePath());
        } else {
            try {
                ClassPathResource resource = new ClassPathResource(configFileLocation);
                Path baseDir = Paths.get(new File(".").getCanonicalPath()); // Current working directory
                if (!Files.exists(baseDir)) { // Should not happen, but defensive
                    Files.createDirectories(baseDir);
                }
                externalConfigPath = baseDir.resolve(configFileLocation).normalize();

                if (!resource.exists()) {
                    logger.warn("Configuration file '{}' not found in classpath. Creating an empty default at: {}", configFileLocation, externalConfigPath.toAbsolutePath());
                    if (externalConfigPath.getParent()!=null && !Files.exists(externalConfigPath.getParent())) {
                        Files.createDirectories(externalConfigPath.getParent());
                    }
                    ProjectConfigs emptyConfigs = new ProjectConfigs();
                    objectMapper.writeValue(externalConfigPath.toFile(), emptyConfigs);
                } else {
                    logger.info("Copying configuration file from classpath '{}' to external location: {}", configFileLocation, externalConfigPath.toAbsolutePath());
                     if (externalConfigPath.getParent()!=null && !Files.exists(externalConfigPath.getParent())) {
                        Files.createDirectories(externalConfigPath.getParent());
                    }
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, externalConfigPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                logger.error("Error initializing configuration file setup from classpath '{}': {}. Trying to create a default.", configFileLocation, e.getMessage());
                 try {
                    Path baseDir = Paths.get(new File(".").getCanonicalPath());
                    externalConfigPath = baseDir.resolve(configFileLocation).normalize();
                    if (externalConfigPath.getParent()!=null && !Files.exists(externalConfigPath.getParent())) {
                        Files.createDirectories(externalConfigPath.getParent());
                    }
                    ProjectConfigs emptyConfigs = new ProjectConfigs();
                    objectMapper.writeValue(externalConfigPath.toFile(), emptyConfigs);
                    logger.info("Created fallback empty default config at: {}", externalConfigPath.toAbsolutePath());
                } catch (IOException ex) {
                     logger.error("FATAL: Could not create or initialize project configuration at all.", ex);
                     externalConfigPath = null;
                }
            }
        }
        loadConfigurations();
    }

    /**
     * Periodically checks the external configuration file for modifications.
     * If changes are detected, it reloads the project configurations. This method
     * is scheduled to run at a fixed delay.
     */
    @Scheduled(fixedDelayString = "${javaseeker.config.watch.delay:5000}", initialDelay = 10000)
    public void checkForUpdates() {
        if (externalConfigPath == null || !Files.exists(externalConfigPath)) {
            logger.trace("External config path not set or file does not exist, skipping update check.");
            return;
        }
        lock.lock();
        try {
            long currentModifiedTime = Files.getLastModifiedTime(externalConfigPath).toMillis();
            if (currentModifiedTime > lastModifiedTime) {
                logger.info("Configuration file {} has changed. Reloading.", externalConfigPath.toAbsolutePath());
                loadConfigurations();
                // eventPublisher.publishEvent(new ProjectConfigurationChangedEvent(this)); // Optional
            }
        } catch (IOException e) {
            logger.error("Error checking configuration file for updates: {}", externalConfigPath.toAbsolutePath(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Loads project configurations from the {@code externalConfigPath} YAML file.
     * Clears existing configurations and populates the internal map with new entries.
     * Normalizes {@code localCachePath} to be absolute and ensures cache directories exist.
     */
    private void loadConfigurations() {
        if (externalConfigPath == null || !Files.exists(externalConfigPath)) {
             logger.warn("Cannot load configurations, externalConfigPath is not valid or file does not exist: {}", externalConfigPath);
            return;
        }
        lock.lock();
        try (InputStream is = Files.newInputStream(externalConfigPath)) {
            ProjectConfigs configs = objectMapper.readValue(is, ProjectConfigs.class);
            Map<String, ProjectEntry> newConfigMap = new ConcurrentHashMap<>();
            Path appBaseDir = Paths.get(new File(".").getCanonicalPath());

            if (configs.getProjects() != null) {
                for (ProjectEntry entry : configs.getProjects()) {
                    if (entry.getName() == null || entry.getName().trim().isEmpty()) {
                        logger.warn("Skipping project entry with no name: {}", entry);
                        continue;
                    }

                    if (entry.getLocalCachePath() == null || entry.getLocalCachePath().trim().isEmpty()) {
                        Path defaultCacheDir = appBaseDir.resolve(".cache").resolve(entry.getName()).normalize();
                        entry.setLocalCachePath(defaultCacheDir.toString());
                    } else {
                        Path cachePath = Paths.get(entry.getLocalCachePath());
                        if (!cachePath.isAbsolute()) {
                            entry.setLocalCachePath(appBaseDir.resolve(cachePath).normalize().toString());
                        }
                    }
                    Path cacheDir = Paths.get(entry.getLocalCachePath());
                    if(!Files.exists(cacheDir)){
                        Files.createDirectories(cacheDir);
                    }

                    if (entry.getStatus() == null) entry.setStatus("NOT_SYNCED"); // Default if not in YAML
                    ProjectEntry existingEntry = projectConfigMap.get(entry.getName());
                    if (existingEntry != null && entry.getStatus().equals("NOT_SYNCED")) {
                         // If reloading, and new config doesn't specify status, preserve old status
                         // This prevents a READY project from being reset to NOT_SYNCED on a simple config comment change
                        entry.setStatus(existingEntry.getStatus());
                        logger.debug("Preserved status '{}' for reloaded project '{}'", existingEntry.getStatus(), entry.getName());
                    }
                    newConfigMap.put(entry.getName(), entry);
                }
            }

            projectConfigMap.clear();
            projectConfigMap.putAll(newConfigMap);

            logger.info("Loaded {} project configurations from {}", projectConfigMap.size(), externalConfigPath.toAbsolutePath());
            if (Files.exists(externalConfigPath)) { // Check again as file might be deleted during read
                 lastModifiedTime = Files.getLastModifiedTime(externalConfigPath).toMillis();
            }

        } catch (IOException e) {
            logger.error("Error loading project configurations from {}: {}", externalConfigPath.toAbsolutePath(), e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a copy of the list of all currently loaded project configurations.
     * The returned list is a snapshot and is safe to iterate over, but modifications
     * to the list itself will not affect the underlying configuration map.
     * Project entries within the list are the actual managed instances.
     * @return A new {@link ArrayList} containing all {@link ProjectEntry} objects.
     *         Returns an empty list if no projects are configured.
     */
    public List<ProjectEntry> getAllProjects() {
        lock.lock();
        try {
            return new ArrayList<>(projectConfigMap.values()); // Return a copy for thread safety against modifications of the list itself
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a specific project configuration by its unique name.
     * @param name The unique name of the project to retrieve. Must not be null.
     * @return An {@link Optional} containing the {@link ProjectEntry} if a project
     *         with the given name exists, otherwise an empty {@link Optional}.
     */
    public Optional<ProjectEntry> getProject(String name) {
        return Optional.ofNullable(projectConfigMap.get(name)); // ConcurrentHashMap allows safe reads
    }

    /**
     * Updates the in-memory lifecycle status of a specified project.
     * This method is thread-safe. The change in status is not persisted
     * back to the {@code javaseeker-projects.yml} file; it only affects the
     * current runtime state of the project entry managed by this service.
     *
     * @param projectName The unique name of the project whose status is to be updated. Must not be null.
     * @param status The new status string to set for the project (e.g., "COMPILING", "READY", "FAILED_SYNC").
     *               It's up to the caller to ensure the status string is valid.
     */
    public void updateProjectStatus(String projectName, String status) {
        lock.lock();
        try {
            ProjectEntry entry = projectConfigMap.get(projectName);
            if (entry != null) {
                String oldStatus = entry.getStatus();
                entry.setStatus(status);
                logger.info("Project '{}' status changed from '{}' to '{}'", projectName, oldStatus, status);
            } else {
                logger.warn("Attempted to update status for unknown project: {}", projectName);
            }
        } finally {
            lock.unlock();
        }
    }
}
