package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

// 모금 서비스 - 단일 기록자: co_fundings·participants.
// 개설 API(controller·service) + ④⑦ 사가 리스너 + ⏰기한 스케줄러 + 발행(SagaPublisher) 완전체.
// :support 전체가 아니라 필요한 패키지만 스캔한다 - web 전용 인프라 빈을 끌고 오지 않기 위해서다.
// survey·payment 리포는 교차 "읽기"(개설 검증·내 주문 조회) 전용 - 쓰기는 각 소유 서비스 몫.
// @EnableScheduling: 기한 무산 스케줄러 + Outbox 릴레이(발행 재시도 보장 - outbox 테이블은 공유,
// web 릴레이와 SKIP LOCKED 로 분업).
@SpringBootApplication(
    scanBasePackages = [
        "com.sbl.sulmun2yong.cofunding",
        "com.sbl.sulmun2yong.global.kafka",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.sbl.sulmun2yong.cofunding.repository",
        "com.sbl.sulmun2yong.global.kafka.outbox.repository",
        "com.sbl.sulmun2yong.survey.repository",
        "com.sbl.sulmun2yong.payment.repository",
    ],
)
@EntityScan(
    basePackages = [
        "com.sbl.sulmun2yong.cofunding.entity",
        "com.sbl.sulmun2yong.global.kafka.outbox.entity",
        "com.sbl.sulmun2yong.survey.entity",
        "com.sbl.sulmun2yong.payment.entity",
    ],
)
@EnableScheduling
class CoFundingApplication

fun main(args: Array<String>) {
    runApplication<CoFundingApplication>(*args)
}
