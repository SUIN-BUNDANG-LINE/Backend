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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.seleniumhq.selenium:selenium-java:4.23.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.9.1")

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

    // New Relic
    implementation("com.newrelic.agent.java:newrelic-agent:8.13.0")
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
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
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
        tags =
            // 프로덕션 배포면 latest와 prodYYMMDDhhmm 태그를 붙히고, 개발 배포면 devLatest와 devYYMMDDhhmm 태그를 붙인다.
            let {
                // main 브랜치 배포 = prod, develop 브랜치 배포 = dev
                val tagName = project.findProperty("DEPLOY_TYPE") as String?
                setOf(tagName + getCurrentDateTime(), if (tagName == "prod") "prodLatest" else "devLatest")
            }
    }
    container {
        jvmFlags = listOf("-Xms128m", "-Xmx128m")
        val newRelicConfig = project.file("newrelic.yml")
        val newRelicJar = project.file("newrelic.jar")
        if (newRelicConfig.exists()) {
            jvmFlags =
                listOf(
                    "-Xms128m",
                    "-Xmx128m",
                    "-Dnewrelic.config.file=${newRelicConfig.absolutePath}",
                    "-javaagent:${newRelicJar.absolutePath}",
                )
        }
    }
}
