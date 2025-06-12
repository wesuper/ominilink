plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    // buildlogic.java-conventions is likely applied to the sub-module directly
    // and it brings Spring Boot plugin.
    // The io.spring.dependency-management plugin is also not needed due to platform BOMs.
}

// Disable bootJar for this aggregator module itself
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// Removed allprojects repositories block

subprojects {
    apply(plugin = "org.springframework.boot") // This might be redundant if buildlogic.java-conventions does it.
    apply(plugin = "io.spring.dependency-management")

    tasks.bootJar {
        enabled = false
    }
}

project(":omnilink-bridge:omnilink-bridge-javaseeker") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.wesuper.liteai.bridge.javaseeker.McpJavaSeekerApplication")
    }
} 