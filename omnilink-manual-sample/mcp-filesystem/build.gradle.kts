dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Spring AI MCP
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation("org.springframework.ai:spring-ai-mcp-core")
    implementation("org.springframework.ai:spring-ai-mcp-client")
    
    // Commons IO for file operations
    implementation("commons-io:commons-io:2.15.1")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.bootJar {
    enabled = true
    mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
} 