plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

// Disable bootJar for this aggregator module itself
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

//allprojects {
//    repositories {
//        mavenCentral()
//    }
//}

subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    tasks.bootJar {
        enabled = false
    }
}

project(":omnilink-flywise:omnilink-flywise-stock") {
    tasks.bootJar {
        enabled = true
        mainClass.set("com.alibaba.spring.ai.example.stock.StockServerApplication")
    }
}

project(":omnilink-flywise:omnilink-flywise-weather") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.sample.server.McpServerApplication")
    }
} 