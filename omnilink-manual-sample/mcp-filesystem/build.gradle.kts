plugins {
    id("buildlogic.java-conventions") // Assumed to apply org.springframework.boot and java
    alias(libs.plugins.org.springframework.boot) // Explicitly add for clarity and to ensure it's there
    application // Add application plugin
}

// Repositories are defined in settings.gradle.kts

dependencies {
    // BOMs are applied by buildlogic.java-conventions plugin.
    // Removed explicit platform() declarations from here.

    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web") // Keep as it was there
    implementation("org.springframework.boot:spring-boot-starter-webflux") // Keep as it was there
    // implementation(libs.org.projectreactor.reactor.core) // Removed redundant declaration, explicit version below
    
    // Alibaba Spring AI
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter

    // Spring AI MCP
    // Removed libs.org.springframework.ai.spring.ai.mcp
    // Removed libs.org.springframework.ai.spring.ai.mcp.core
    // implementation(libs.org.springframework.ai.spring.ai.mcp.client.webflux.spring.boot.starter) // Removed MCP Starter
    implementation("io.projectreactor:reactor-core:3.6.5")  // Explicit version for diagnosis
    
    // Commons IO for file operations
    implementation(libs.commons.io) // Use libs alias (assuming commons-io = "2.15.1" is in libs.versions.toml)
}

// Removed separate dependencies block for reactor-core

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

application {
    mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
}

// Replicate maven-resources-plugin behavior
val copyData = tasks.register<Copy>("copyData") {
    from("data")
    into(layout.buildDirectory.dir("data")) // Copies into build/data
}

tasks.processResources {
    dependsOn(copyData)
    from(layout.buildDirectory.dir("data")) {
        into("data") // To make it available in classpath under 'data' if needed by app
    }
}

// Ensure bootJar is configured if not already by conventions
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
}