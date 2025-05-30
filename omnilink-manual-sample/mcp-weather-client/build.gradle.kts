plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
    application
}

description = "Spring AI - MCP WEBFLUX Client EXAMPLE"

// Repositories are defined in settings.gradle.kts

dependencies {
    // implementation(libs.org.springframework.ai.spring.ai.mcp.client.webflux.spring.boot.starter) // Removed MCP Starter
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter
}

application {
    mainClass.set("com.alibaba.cloud.ai.example.mcp.webflux.WeatherClientApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    // mainClass is set via application plugin
}
