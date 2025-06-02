plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
    application
}

description = "Simple AI Application using MCP client to chat with SQLite"

// Repositories are defined in settings.gradle.kts

dependencies {
    // Assuming libs.spring.ai.bom is configured to 1.0.0-M5 or thereabouts
    // Or a more specific BOM alias exists for M5. For now, using the general one.
    implementation(platform(libs.spring.ai.bom)) // Corrected alias

    implementation(libs.org.springframework.boot.spring.boot.starter)
    // Ensure libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter points to a version compatible with 1.0.0-M5.1
    // implementation(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter
    // implementation(libs.org.springframework.ai.spring.ai.mcp.client.webflux.spring.boot.starter) // Removed MCP starter
    implementation(libs.spring.ai.starter.model.openai)
    implementation(libs.org.springframework.boot.spring.boot.starter.jdbc)
    runtimeOnly(libs.sqlite.jdbc)

    // Dependencies for SQLite that are likely needed but not in mcp-sqlite's pom directly
    // These would typically be in a project that *uses* SQLite, not one *providing a client to it*.
    // However, this sample might be a self-contained demo.
    // implementation("org.xerial:sqlite-jdbc") ?
    // implementation("org.springframework.boot:spring-boot-starter-data-jpa") or jdbc ?
    // For now, sticking to what's in the pom. If build fails, these might be candidates.
}

application {
    mainClass.set("org.springframework.ai.mcp.sqlite.SqliteApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

// Java version (e.g., 17 or 21) is expected to be set by buildlogic.java-conventions
// spring.ai.alibaba version 1.0.0-M5.1 needs to be ensured via libs.versions.toml for the alias used.
