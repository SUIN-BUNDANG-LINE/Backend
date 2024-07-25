import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getCurrentDateTime(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"))

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
//    kotlin("plugin.jpa") version "1.9.24"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("com.google.cloud.tools.jib") version "3.4.0"
    id("org.sonarqube") version "4.4.1.3373"
    jacoco
}

group = "com.sbl"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // TODO: 프론트엔드와 연동되면 삭제 예정
    // MVC
    implementation("org.springframework.boot:spring-boot-starter-mustache")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // jpa (RDBMS 사용 전 까지는 비활성화)
    // implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // mysql (MySQL 사용 전 까지는 비활성화)
    // runtimeOnly("com.mysql:mysql-connector-j")

    // mongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Swagger
    implementation("org.springdoc:springdoc-openapi:2.3.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // test
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        html.required.set(false)
        xml.required.set(true)
        csv.required.set(false)
    }
}

sonarqube {
    properties {
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            project.layout.buildDirectory
                .file("reports/jacoco/test/jacocoTestReport.xml")
                .get()
                .asFile.absolutePath,
        )
        property("sonar.projectKey", "SUIN-BUNDANG-LINE_Backend_AZDrXmI4FBe8ovYIAI-X")
        property("sonar.projectName", "Backend")
    }
}

jib {
    from {
        image = "openjdk:17-slim"
        auth {
            username = project.findProperty("DOCKER_ID") as String?
            password = project.findProperty("DOCKER_PASSWORD") as String?
        }
    }
    to {
        image = "${project.findProperty("DOCKER_ID")}/${project.findProperty("DOCKER_IMAGE_NAME")}"
        auth {
            username = project.findProperty("DOCKER_ID") as String?
            password = project.findProperty("DOCKER_PASSWORD") as String?
        }
        tags = setOf("latest", getCurrentDateTime())
    }
    container {
        jvmFlags = listOf("-Xms128m", "-Xmx128m")
    }
}
