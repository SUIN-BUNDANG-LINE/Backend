plugins {
    // Gradle Toolchain 자동 해석/다운로드 — 시스템에 JDK 17이 없으면 자동 다운로드
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "sulmun2yong"

include("common", "web")

// 두 모듈 모두 modules/ 아래에 위치 (모듈 이름은 :common, :web 유지)
// :consumer 는 별도 레포지토리(sulmoon2yong-consumer)로 분리됨
project(":common").projectDir = file("modules/common")
project(":web").projectDir = file("modules/web")
