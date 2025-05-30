plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

// Disable bootJar for this aggregator module itself
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// Removed problematic version properties and allprojects repositories block.
// BOMs are applied via buildlogic.java-conventions.

subprojects {
    apply(plugin = "org.springframework.boot") // Potentially redundant if buildlogic.java-conventions is used by subprojects
    apply(plugin = "io.spring.dependency-management") // Redundant due to platform BOMs

    tasks.bootJar {
        enabled = false
    }

    // Removed redundant dependencyManagement block.
    // Versions/alignments come from BOMs applied by buildlogic.java-conventions.

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.springframework.boot:spring-boot-starter-web") // If truly common
        implementation("org.springframework.boot:spring-boot-starter-webflux") // If truly common
        // Removed: implementation("org.projectreactor:reactor-core") - submodules should declare with libs
        implementation("com.fasterxml.jackson.core:jackson-databind") // If truly common
        implementation("io.projectreactor.netty:reactor-netty") // If truly common
        // Removed: implementation("org.springframework.ai:spring-ai-mcp") - submodules should declare with libs
    }
}

project(":omnilink-manual-sample:mcp-stock-client") {
    tasks.bootJar {
        enabled = true
        mainClass.set("com.alibaba.cloud.ai.example.mcp.streamable.Application")
    }
}

project(":omnilink-manual-sample:mcp-github") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
    }
}

project(":omnilink-manual-sample:mcp-filesystem") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
    }
}

project(":omnilink-manual-sample:mcp-weather-client") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.client.Application")
    }
}

project(":omnilink-manual-sample:mcp-javaseeker-client") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.client.Application")
    }
} 