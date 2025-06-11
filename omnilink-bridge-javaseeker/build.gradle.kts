plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.ai:spring-ai-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("fr.inria.gforge.spoon:spoon-core:11.0.0") // Spoon for AST analysis
    implementation(libs.jgit) // Added JGit dependency
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter) // For @ExtendWith(MockitoExtension.class)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:0.8.0")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
