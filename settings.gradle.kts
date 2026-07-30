plugins {
    // Gradle Toolchain 자동 해석/다운로드 — 시스템에 JDK 17이 없으면 자동 다운로드
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "sulmun2yong"

include("support", "produce", "web")
// :support — 도메인/엔티티/리포지토리 + 공유 기반(global error·data·util·converter·annotation, oauth2 provider) + 비프로듀서 도메인 로직(ai/aws/notification/user). 기반 라이브러리.
// :produce — Kafka 를 produce 하는 도메인(drawing/survey)의 서비스/퍼블리셔/컨트롤러 + outbox·publisher·kafka config + 분산락. :support 에 의존.
// :web    — 실행 진입점(SpringBootApplication) + 비프로듀서 도메인 컨트롤러(ai/aws/user) + 보안/JWT/resolver/전역 config. :produce, :support 에 의존.
project(":support").projectDir = file("modules-web/support")
project(":produce").projectDir = file("modules-web/produce")
project(":web").projectDir = file("modules-web/web")

// ── Kafka 컨슈머 계열 (module-consumer/) — MSA 자족형. 각자 SpringBootApplication/bootJar/이미지를 가진 독립 서비스.
// :common 은 컨슈머가 공유하는 DTO 라이브러리(프로듀서 계열의 :support 와 별개). 각 컨슈머는 :common 에만 의존한다.
include(
    "common",
    "drawing-sms-notification-consumer",
    "dlt-sms-notification-consumer",
    "co-funding-consumer",
)
project(":common").projectDir = file("module-consumer/common")
project(":drawing-sms-notification-consumer").projectDir =
    file("module-consumer/drawing-sms-notification-consumer")
project(":dlt-sms-notification-consumer").projectDir =
    file("module-consumer/dlt-sms-notification-consumer")
project(":co-funding-consumer").projectDir =
    file("module-consumer/co-funding-consumer")
