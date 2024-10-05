import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // fingerprint
    implementation("com.github.fingerprintjs:fingerprint-pro-server-api-java-sdk:v6.0.0")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // jpa (RDBMS 사용 전 까지는 비활성화)
    // implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // mysql (MySQL 사용 전 까지는 비활성화)
    // runtimeOnly("com.mysql:mysql-connector-j")

    // mongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // mongock
    implementation("io.mongock:mongock-springboot:5.4.4")
    implementation("io.mongock:mongodb-springdata-v4-driver:5.4.4")

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

    // New Relic
    implementation("com.newrelic.agent.java:newrelic-agent:8.13.0")
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
