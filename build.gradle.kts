import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.plugins.JavaPluginExtension // Required for sourceSets if used explicitly
import org.gradle.kotlin.dsl.the // Required for extensions.getByType

// Ensure libs is available throughout this script
val libs = the<LibrariesForLibs>()

plugins {
    alias(libs.plugins.spring.boot) apply false // Using alias from libs
    java // Basic Java plugin
    `java-library` // For defining API and implementation configurations
    idea // For IntelliJ IDEA project files
    // Removed io.spring.dependency-management as it's not typically needed with Gradle's platform()
}

allprojects {
    group = "org.wesuper.liteai"
    version = "1.0.0"

    // Repositories are now defined in settings.gradle.kts
    // Removing this block to comply with RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

subprojects {
    apply(plugin = "java") // Apply java to all subprojects
    apply(plugin = "java-library") // Apply java-library to all subprojects
    apply(plugin = "org.springframework.boot") // Apply spring boot to all subprojects that need it (is this too broad?)
                                            // Convention plugins in each module might be more targeted.
                                            // For now, keeping it as per original structure, but it means non-Spring Boot modules also get it.
                                            // Consider if buildlogic.java-conventions should handle applying this.
    // Removed apply io.spring.dependency-management

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.release.set(21)
    }
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    // Removed extra properties for versions, should come from libs.versions.toml via aliases

    dependencies {
        implementation(platform(rootProject.project.libs.spring.boot.dependencies))
        implementation(platform(rootProject.project.libs.spring.ai.bom))
        // BOMs (spring-boot-dependencies, spring-ai-bom) are now applied via
        // the buildlogic.java-conventions plugin for projects that use it.
        // No need to apply them globally here if all relevant projects use that convention.

        // Removed global application of com.alibaba.cloud.ai:spring-ai-alibaba-starter
        // This should be added specifically to modules that need it.
    }
}