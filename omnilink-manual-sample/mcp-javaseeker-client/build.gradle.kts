plugins {
    // id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
    application
}

description = "Spring AI - MCP STDIO Client EXAMPLE"

// Repositories are defined in settings.gradle.kts

dependencies {
    // Spring AI BOM should be inherited from parent or project if used consistently,
    // but pom.xml doesn't show it. Add if needed for version consistency, or rely on explicit versions.
    // For now, using versions specified in this module's original pom.
    // implementation(libs.spring.ai.mcp.client.spring.boot.starter) // Removed MCP Starter
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter
    implementation(libs.org.springframework.boot.spring.boot.starter)
    implementation(libs.spring.ai.starter.model.openai)
}

// Java version (e.g., 21) is expected to be set by buildlogic.java-conventions
// Application main class will be auto-detected by Spring Boot.
// tasks.bootJar { mainClass.set("your.main.class.here") } // Only if auto-detection fails
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    // mainClass is typically auto-detected by Spring Boot plugin.
}
