package org.wesuper.liteai.bridge.javaseeker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Omnilink Bridge JavaSeeker MCP Server.
 * <p>
 * This application is configured to run as a headless service (WebApplicationType.NONE),
 * enabling component scanning for all sub-packages and scheduling for background tasks
 * like project lifecycle management.
 * </p>
 */
@SpringBootApplication(scanBasePackages = {"org.wesuper.liteai.bridge.javaseeker"})
@EnableScheduling
public class McpJavaSeekerApplication {

    /**
     * Main entry point for the Spring Boot application.
     * Sets the application to run as a non-web application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(McpJavaSeekerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // Ensure headless operation
        app.run(args);
    }
}
