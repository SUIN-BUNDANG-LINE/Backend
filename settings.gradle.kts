plugins {
    // Gradle Toolchain 자동 해석/다운로드 — 시스템에 JDK 17이 없으면 자동 다운로드
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "sulmun2yong"

include("common", "web", "consumer")

// web/consumer는 modules/ 아래에 위치 (모듈 이름은 :web, :consumer 유지)
project(":web").projectDir = file("modules/web")
project(":consumer").projectDir = file("modules/consumer")
