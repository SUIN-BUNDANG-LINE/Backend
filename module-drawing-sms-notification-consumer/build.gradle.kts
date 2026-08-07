import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getCurrentDateTime(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"))

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.jpa")
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

    // OpenTelemetry API — 트레이스 스팬에 saga.id 속성을 붙여 Tempo 에서 사가 키로 검색 가능하게 한다.
    // 에이전트(-javaagent 2.28.x)는 api 호출을 '계측'만 할 뿐 api 인터페이스 클래스를 앱에 주입하지 않는다.
    // 따라서 api 는 반드시 앱 classpath 에 번들해야 한다 → implementation (compileOnly 로 두면 런타임 NoClassDefFound).
    implementation("io.opentelemetry:opentelemetry-api:1.40.0")

    // ===== Spring stack — 컨슈머가 자급하는 인프라 (consumer-base 분해 후 인라인) =====
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // JPA + MySQL + Flyway (백엔드와 같은 schema)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Redis + Redisson 분산락 + AOP
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson:3.45.1")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // EncryptionUtils / EncryptedStringConverter — Spring Security crypto BytesEncryptor 만 사용
    implementation("org.springframework.security:spring-security-crypto")

    // management-only Tomcat (Actuator/Prometheus 전용 — 컨트롤러 없음)
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
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
    mainClass.set("com.sbl.sulmun2yong.SmsNotificationConsumerApplicationKt")
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
        image = "eclipse-temurin:17-jre"
        auth {
            username = project.findProperty("DOCKER_ID") as String?
            password = project.findProperty("DOCKER_PASSWORD") as String?
        }
    }
    to {
        image =
            "${project.findProperty("DOCKER_ID")}/${project.findProperty("DOCKER_IMAGE_NAME")}-drawing-sms-notification-consumer"
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
