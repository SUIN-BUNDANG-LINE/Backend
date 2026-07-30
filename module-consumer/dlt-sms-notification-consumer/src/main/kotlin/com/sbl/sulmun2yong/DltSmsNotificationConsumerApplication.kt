package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// drawing-notification.DLT 토픽을 dlt-sms-notification groupId로 구독하는 독립 consumer 진입점.
// 책임: 최종 실패한 SMS 메시지를 dlt_messages 테이블로 영속화.
// 패키지 루트 배치 - 기본 스캔이 전 하위 패키지를 덮으므로 스캔 범위 수동 확장이 필요 없다
@SpringBootApplication
class DltSmsNotificationConsumerApplication

fun main(args: Array<String>) {
    runApplication<DltSmsNotificationConsumerApplication>(*args)
}
