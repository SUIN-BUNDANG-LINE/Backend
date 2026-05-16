plugins {
    id("org.springframework.boot") version "3.3.1" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("kapt") version "1.9.24" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("com.google.cloud.tools.jib") version "3.4.0" apply false
}

allprojects {
    group = "com.sbl"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
