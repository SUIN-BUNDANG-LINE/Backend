plugins {
    // Gradle Toolchain 자동 해석/다운로드 — 시스템에 JDK 17이 없으면 자동 다운로드
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "sulmun2yong"

// ── 공유 기반 (module-support/) — 도메인/엔티티/리포지토리 + 공유 기반(global error·data·util·converter,
// kafka·outbox·분산락 인프라, oauth2 provider) + notification 도메인 로직.
// (ai/aws/user 서비스 계층은 :survey-drawing 소속 — 여기엔 데이터 계층(entity·repository·dto·domain)만)
// 모든 도메인 서비스(web·auth·cofunding·payment)가 의존하는 기반 라이브러리.
include("support")
project(":support").projectDir = file("module-support")

// ── 설문·추첨 서비스 (module-survey-drawing/) — 실행 진입점(SpringBootApplication) + 설문/추첨 도메인
// (서비스·퍼블리셔·컨트롤러·사가 리스너) + 비프로듀서 도메인 컨트롤러(ai/aws/user) +
// 보안/JWT/resolver/전역 config. 다른 서비스와 같은 최상위 단일 모듈. :support 에 의존.
include("survey-drawing")
project(":survey-drawing").projectDir = file("module-survey-drawing")

// ── Kafka 컨슈머 계열 — MSA 자족형. 각자 SpringBootApplication/bootJar/이미지를 가진 독립 서비스.
// :common 은 컨슈머가 공유하는 DTO 라이브러리(:support 와 별개). 각 컨슈머는 :common 에만 의존한다.
include(
    "common",
    "drawing-sms-notification-consumer",
    "dlt-sms-notification-consumer",
)
project(":common").projectDir = file("module-common")
project(":drawing-sms-notification-consumer").projectDir =
    file("module-drawing-sms-notification-consumer")
project(":dlt-sms-notification-consumer").projectDir =
    file("module-dlt-sms-notification-consumer")

// ── API 게이트웨이 (module-gateway/) — 인증(JWT 검증)·라우팅 전용. Spring Cloud Gateway(WebFlux).
// 도메인 서비스는 게이트웨이가 붙인 X-User-Id 헤더만 신뢰한다. 자족형 독립 서비스.
include("gateway")
project(":gateway").projectDir = file("module-gateway")

// ── 모금 서비스 (module-cofunding/) — 단일 기록자: co_fundings·participants. 완전체(Phase 3):
// 개설 API + ④ 정산(SETTLED+장벽 CAS)·⑦ 환불 수렴 리스너 + ⏰기한 무산 스케줄러 + 사가 발행(②⑤⑥⑧).
include("cofunding")
project(":cofunding").projectDir = file("module-cofunding")

// ── 결제 서비스 (module-payment/) — 단일 기록자: payment_orders·commands·webhook_inbox. 완전체(Phase 4):
// confirm 착지·webhook·checkout-info API + 커맨드 릴레이(토스 자력 발송) + ②⑥⑧ 리스너 + 사실 발행(④⑦·failed).
include("payment")
project(":payment").projectDir = file("module-payment")

// ── 인증 서비스 (module-auth/) — OAuth2 로그인·JWT 발급·리프레시 전담.
// 게이트웨이는 검증만, auth 는 발급만, 도메인 서비스는 PURE(헤더만 신뢰)로 가는 삼분 구조.
// user/refresh 엔티티·리포지토리는 :support 공유. 자족형 독립 서비스.
include("auth")
project(":auth").projectDir = file("module-auth")
