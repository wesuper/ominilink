dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // MCP Core Dependencies
    implementation("org.springframework.ai:spring-ai-mcp-client")
    implementation("org.springframework.ai:spring-ai-mcp-spec")
    implementation("org.springframework.ai:spring-ai-mcp-transport")
    implementation("org.springframework.ai:spring-ai-mcp-transport-http")
    implementation("org.springframework.ai:spring-ai-mcp-transport-websocket")
    implementation("org.springframework.ai:spring-ai-mcp-common")
}

tasks.bootJar {
    enabled = true
    mainClass.set("com.alibaba.cloud.ai.example.mcp.streamable.Application")
} 