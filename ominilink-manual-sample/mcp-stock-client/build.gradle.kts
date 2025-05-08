dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Reactor & Jackson
    implementation("io.projectreactor:reactor-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.projectreactor.netty:reactor-netty")
    
    // MCP Core Dependencies
    implementation("org.springframework.ai:spring-ai-mcp-spec:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-core:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-client:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-transport:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-schema:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-client-webflux:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-client-spring-boot-starter:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-mcp-client-webflux-spring-boot-starter:1.0.0-M6")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // Utils
    implementation("org.springframework.ai:spring-ai-mcp-utils:1.0.0-M6")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.bootJar {
    enabled = true
    mainClass.set("com.alibaba.cloud.ai.example.mcp.streamable.Application")
} 