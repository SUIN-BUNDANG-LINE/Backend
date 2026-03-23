import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getCurrentDateTime(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"))

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("plugin.jpa") version "1.9.24"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("kapt") version "1.9.24"
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
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // fingerprint
    // implementation("com.github.fingerprintjs:fingerprint-pro-server-api-java-sdk:v6.0.0")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // MySQL
    runtimeOnly("com.mysql:mysql-connector-j")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Swagger
    implementation("org.springdoc:springdoc-openapi:2.3.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // AWS
    implementation("software.amazon.awssdk:bom:2.27.24")
    implementation("software.amazon.awssdk:s3:2.27.24")

    // test
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")

    // New Relic
    implementation("com.newrelic.agent.java:newrelic-agent:8.13.0")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson:3.45.1")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform {
        excludeTags("concurrency")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("concurrencyTest") {
    useJUnitPlatform {
        includeTags("concurrency")
    }
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
            // 운영 배포면 현재 버전에 대한 태그를 붙힌다., 개발 배포면 devLatest와 YYMMDDhhmm 태그를 붙인다.
            let {
                val versionName = project.findProperty("VERSION") as String?
                if (versionName != null) {
                    setOf(versionName)
                } else {
                    setOf(getCurrentDateTime(), "devLatest")
                }
            }
    }
    container {
        // JVM 메모리 설정
        jvmFlags =
            listOf(
                "-Xms${project.findProperty("JVM_XMS")}",
                "-Xmx${project.findProperty("JVM_XMX")}",
            )
        // New Relic 설정
        val newRelicConfig = project.file("newrelic/newrelic.yml")
        val newRelicJar = project.file("newrelic/newrelic.jar")
        if (newRelicConfig.exists() && newRelicJar.exists()) {
            jvmFlags =
                listOf(
                    "-Xms${project.findProperty("JVM_XMS")}",
                    "-Xmx${project.findProperty("JVM_XMX")}",
                    "-Dnewrelic.config.file=/app/config/newrelic.yml",
                    "-javaagent:/app/config/newrelic.jar",
                )
        }
    }
    // New Relic 설정
    extraDirectories {
        paths {
            path {
                setFrom(file("newrelic").toPath())
                into = "/app/config"
            }
        }
    }
}
