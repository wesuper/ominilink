plugins {
    // id("buildlogic.java-conventions")
    alias(libs.plugins.org.springframework.boot)
}

description = "Spring AI Alibaba Register MCP Client To Nacos Example"

dependencies {
//    implementation(libs.spring.ai.mcp.server.webflux)
    implementation(libs.spring.boot.starter.logging)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.ai.starter.model.openai)
    implementation(libs.spring.ai.autoconfigure.model.chat.client)
    // api("com.alibaba.cloud.ai:spring-ai-alibaba-mcp-nacos:1.0.0-M6.1-SNAPSHOT") // Removed SNAPSHOT dependency
    // api(libs.spring.ai.mcp.server.webmvc.spring.boot.starter) // Removed MCP Starter
//    implementation(libs.spring.ai.alibaba.starter)
//
    implementation(libs.spring.ai.alibaba.mcp.client.nacos)
}
