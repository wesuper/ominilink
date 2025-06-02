plugins {
    id("buildlogic.java-conventions") // Assumed to apply org.springframework.boot and java
    alias(libs.plugins.org.springframework.boot)
    application // For Spring Boot main class auto-detection and standard tasks
}

// Repositories are defined in settings.gradle.kts

dependencies {
    // Spring AI BOM is applied by buildlogic.java-conventions plugin.
    // Removed explicit platform(libs.spring.ai.bom) from here.

    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // Kept from original
    
    // Alibaba Spring AI
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter

    // Spring AI MCP
    // Removed explicit libs.org.springframework.ai.spring.ai.mcp
    // Removed explicit libs.org.springframework.ai.spring.ai.mcp.core
    // implementation(libs.org.springframework.ai.spring.ai.mcp.client.spring.boot.starter) // Removed MCP Starter
    implementation(libs.spring.ai.starter.model.openai) // Add OpenAI Starter
    
    // GitHub API Client
    implementation(libs.kohsuke.github.api) // Use libs alias
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21" // Keep as is
    targetCompatibility = "21" // Keep as is
}

// Spring Boot plugin will automatically find main class if 'application' plugin is applied
// and there's a single main method, or if configured via properties.
// tasks.bootJar {
// enabled = true
// mainClass.set("...") // Let Spring Boot auto-detect or specify if known and unique for this module
// }
// No explicit mainClass needed here for now, let Spring Boot handle it.
// If build fails, main class for this specific module needs to be found and set in application {} block.

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    // mainClass is typically auto-detected by Spring Boot plugin.
    // If specific main class is needed: mainClass.set("your.main.class.Here")
}