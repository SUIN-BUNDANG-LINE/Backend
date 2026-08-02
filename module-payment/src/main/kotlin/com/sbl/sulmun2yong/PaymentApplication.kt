package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

// 결제 서비스 - 단일 기록자: payment_orders·commands·webhook_inbox. 완전체(Phase 4):
// confirm 착지·webhook·checkout-info API + 커맨드 릴레이(토스 confirm/cancel 자력 발송) +
// ②⑥⑧ 사가 리스너 + 사실 발행(PaymentEventPublisher). 토스 어댑터(:support)의 유일한 구동처.
// :support 전체가 아니라 필요한 패키지만 스캔한다 - web 전용 인프라 빈을 끌고 오지 않기 위해서다.
// @EnableScheduling: PaymentCommandRelay + Outbox 릴레이(발행 재시도 - outbox 테이블 공유, SKIP LOCKED 분업).
@SpringBootApplication(
    scanBasePackages = [
        "com.sbl.sulmun2yong.payment",
        "com.sbl.sulmun2yong.global.kafka",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "com.sbl.sulmun2yong.payment.repository",
        "com.sbl.sulmun2yong.global.kafka.outbox.repository",
    ],
)
@EntityScan(
    basePackages = [
        "com.sbl.sulmun2yong.payment.entity",
        "com.sbl.sulmun2yong.global.kafka.outbox.entity",
    ],
)
@EnableScheduling
class PaymentApplication

fun main(args: Array<String>) {
    runApplication<PaymentApplication>(*args)
}
