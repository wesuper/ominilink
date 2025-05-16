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

project(":ominilink-regsvr:ominilink-regsvr-nacos") {
    tasks.bootJar {
        enabled = true
        mainClass.set("com.alibaba.cloud.ai.example.Application")
    }
} 