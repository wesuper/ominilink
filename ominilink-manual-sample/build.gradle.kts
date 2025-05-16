plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

// 继承父项目的版本管理
val springBootVersion: String by project
val springAiVersion: String by project

allprojects {
    group = "org.wesuper.ailite"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.spring.io/milestone")
        }
        maven {
            url = uri("https://repo.spring.io/snapshot")
        }
    }
}

subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    tasks.bootJar {
        enabled = false
    }

    // 使用父项目的版本管理
    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
            mavenBom("org.springframework.ai:spring-ai-bom:${springAiVersion}")
        }
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-webflux")
        implementation("org.projectreactor:reactor-core")
        implementation("com.fasterxml.jackson.core:jackson-databind")
        implementation("io.projectreactor.netty:reactor-netty")
        implementation("org.springframework.ai:spring-ai-mcp")
    }
}

project(":ominilink-manual-sample:mcp-stock-client") {
    tasks.bootJar {
        enabled = true
        mainClass.set("com.alibaba.cloud.ai.example.mcp.streamable.Application")
    }
}

project(":ominilink-manual-sample:mcp-github") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
    }
}

project(":ominilink-manual-sample:mcp-filesystem") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.filesystem.Application")
    }
}

project(":ominilink-manual-sample:mcp-weather-client") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.client.Application")
    }
}

project(":ominilink-manual-sample:mcp-javaseeker-client") {
    tasks.bootJar {
        enabled = true
        mainClass.set("org.springframework.ai.mcp.samples.client.Application")
    }
} 