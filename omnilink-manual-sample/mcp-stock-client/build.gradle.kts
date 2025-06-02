plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
    application
}

description = "Spring AI - MCP STREAMABLE WEBFLUX Client EXAMPLE"

// Repositories are defined in settings.gradle.kts

dependencies {
    // Spring Boot Core (kept from original build.gradle.kts)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(libs.spring.ai.starter.model.openai) // Add OpenAI Starter
    
    // Spring AI MCP Client Starter (replaces granular dependencies)
    // implementation(libs.org.springframework.ai.spring.ai.mcp.client.webflux.spring.boot.starter) // Removed MCP Starter

    // Alibaba Spring AI Starter
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter
}

application {
    mainClass.set("com.alibaba.cloud.ai.example.mcp.streamable.Application")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    // mainClass is set via application plugin
}