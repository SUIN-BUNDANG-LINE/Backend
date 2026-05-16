import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getCurrentDateTime(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"))

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.google.cloud.tools.jib")
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":common"))

    // Web 진입점 전용
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Swagger (web 노출용)
    implementation("org.springdoc:springdoc-openapi:2.3.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Test — @IntegrationTest가 양쪽 진입점 빈을 로딩하므로 consumer 코드도 testCompile 시점에 필요
    testImplementation(project(":consumer"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
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

springBoot {
    mainClass.set("com.sbl.sulmun2yong.Sulmun2yongApplicationKt")
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
        image = "${project.findProperty("DOCKER_ID")}/${project.findProperty("DOCKER_IMAGE_NAME")}-web"
        auth {
            username = project.findProperty("DOCKER_ID") as String?
            password = project.findProperty("DOCKER_PASSWORD") as String?
        }
        tags =
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
        jvmFlags =
            listOf(
                "-Xms${project.findProperty("JVM_XMS")}",
                "-Xmx${project.findProperty("JVM_XMX")}",
            )
    }
}
