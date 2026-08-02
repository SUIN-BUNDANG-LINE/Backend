import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.jpa")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("kapt")
    id("org.jlleitschuh.gradle.ktlint")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// support 모듈은 라이브러리 JAR로만 사용 (Spring Boot fat JAR 비활성)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}
tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}

dependencies {
    // 도메인/공유 기반이 자급하는 인프라 라이브러리 — 과거 :common 이 노출하던 스타터를 support 가 흡수.
    // support 의 public 시그니처가 이 타입들을 노출하고 web 이 그대로 사용하므로 api 로 재전파한다.
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("io.micrometer:micrometer-registry-prometheus")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("org.jetbrains.kotlin:kotlin-reflect")

    // Kafka
    api("org.springframework.kafka:spring-kafka")

    // JPA + MySQL + Flyway
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    api("org.flywaydb:flyway-core")
    api("org.flywaydb:flyway-mysql")

    // QueryDSL (survey/repository 커스텀 구현이 사용)
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // DTO Validation
    api("org.springframework.boot:spring-boot-starter-validation")

    // AWS S3 (도메인 service에서 사용)
    api("software.amazon.awssdk:s3:2.27.24")

    // Redis + Redisson 분산락
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.redisson:redisson:3.45.1")

    // AOP (분산락 어드바이스)
    api("org.springframework.boot:spring-boot-starter-aop")

    // 도메인 service/util에서 사용 — MultipartFile (FileUploadValidator), BytesEncryptor (EncryptionUtils)
    api("org.springframework:spring-web")
    api("org.springframework.security:spring-security-crypto")

    // JWT (token provider — web 진입점이 사용)
    api("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
