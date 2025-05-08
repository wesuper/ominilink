plugins {
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    java
}

allprojects {
    group = "org.wesuper.ailite"
    version = "1.0.0"

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.release.set(21)
    }

    extra["springBootVersion"] = "3.4.5"
    extra["springAiVersion"] = "1.0.0-M6"
    extra["springAiAlibabaVersion"] = "1.0.0-M6.1"

    dependencies {
        implementation(platform("org.springframework.boot:spring-boot-dependencies:${project.extra["springBootVersion"]}"))
        implementation(platform("org.springframework.ai:spring-ai-bom:${project.extra["springAiVersion"]}"))
        implementation("com.alibaba.cloud.ai:spring-ai-alibaba-starter:${project.extra["springAiAlibabaVersion"]}")
    }
} 