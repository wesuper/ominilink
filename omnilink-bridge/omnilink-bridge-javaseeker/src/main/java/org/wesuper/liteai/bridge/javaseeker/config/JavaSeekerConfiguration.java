package org.wesuper.liteai.bridge.javaseeker.config;

import org.springframework.context.annotation.Configuration;

/**
 * General configuration class for the JavaSeeker module.
 * <p>
 * This class can be used to define beans and other configuration settings
 * specific to the JavaSeeker's functionality. Currently, it serves as a
 * placeholder for future configurations and helps establish the package structure.
 * Beans related to specific tools or services may be defined here or in more
 * specialized configuration classes.
 * </p>
 */
@Configuration
public class JavaSeekerConfiguration {
    // Beans for ProjectConfigurationManager, ProjectLifecycleService,
    // and the @Tool annotated analysis service will be primarily auto-discovered
    // via @Service and @Component annotations. This class remains for any explicit
    // bean definitions or component scan fine-tuning if needed in the future.
}
