plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
}

description = "Spring AI Alibaba Register MCP Server To Nacos Example"

dependencies {
    implementation(libs.org.springframework.boot.spring.boot.starter)
    api(libs.org.springframework.boot.spring.boot.starter.web) // Keep as api if it exposes web types, otherwise implementation
    // api(libs.com.alibaba.cloud.ai.spring.ai.alibaba.starter) // Removed Alibaba Starter
    // api("com.alibaba.cloud.ai:spring-ai-alibaba-mcp-nacos:1.0.0-M6.1-SNAPSHOT") // Removed SNAPSHOT dependency
    // api(libs.org.springframework.ai.spring.ai.mcp.server.webmvc.spring.boot.starter) // Removed MCP Starter
    implementation(libs.spring.ai.starter.model.openai)
}
