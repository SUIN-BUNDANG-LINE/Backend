package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// drawing-completed 토픽을 drawing-notification groupId로 구독하는 독립 consumer 진입점.
// 책임: 당첨자 SMS 발송 (잡 생성 + 비동기 발송 + 워커 재시도 + DLT 발행).
// 패키지 루트 배치 - 기본 스캔이 전 하위 패키지를 덮으므로 스캔 범위 수동 확장이 필요 없다
@SpringBootApplication
class SmsNotificationConsumerApplication

fun main(args: Array<String>) {
    runApplication<SmsNotificationConsumerApplication>(*args)
}
