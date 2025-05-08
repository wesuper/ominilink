plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    tasks.bootJar {
        enabled = false
    }
}

project(":ominilink-flywise-stock") {
    tasks.bootJar {
        enabled = true
        mainClass.set("com.alibaba.spring.ai.example.stock.StockServerApplication")
    }
}

project(":ominilink-flywise-weather") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.sample.server.McpServerApplication")
    }
} 