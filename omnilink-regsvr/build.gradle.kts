plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

// Disable bootJar for this aggregator module itself
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// Removed allprojects repositories block

subprojects {
    apply(plugin = "org.springframework.boot") // This might be redundant
    apply(plugin = "io.spring.dependency-management")

    tasks.bootJar {
        enabled = false
    }
}

project(":omnilink-regsvr:omnilink-regsvr-nacos") {
    tasks.bootJar {
        enabled = true
        mainClass.set("com.alibaba.cloud.ai.example.Application")
    }
} 