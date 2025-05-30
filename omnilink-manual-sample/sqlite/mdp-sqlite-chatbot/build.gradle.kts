plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
    application
}

description = "Spring AI - MDP SQLite Chatbot" // Adjusted description

// Repositories are defined in settings.gradle.kts

dependencies {
    // Assuming libs.spring.ai.bom is configured to 1.0.0-M5 or thereabouts
    implementation(platform(libs.spring.ai.bom)) // Corrected alias

    implementation(libs.org.springframework.boot.spring.boot.starter)
    // Ensure libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter points to a version compatible with 1.0.0-M5.1
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter
    // implementation(libs.org.springframework.ai.spring.ai.mcp.client.webflux.spring.boot.starter) // Removed MCP starter
}

application {
    // Tentative main class name based on typical Spring Boot app structure for this module
    mainClass.set("org.springframework.ai.mcp.sqlite.chatbot.ChatbotApplication") 
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

// Java version (e.g., 17 or 21) is expected to be set by buildlogic.java-conventions
// spring.ai.alibaba version 1.0.0-M5.1 needs to be ensured via libs.versions.toml for the alias used.
