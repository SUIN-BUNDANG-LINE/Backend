package com.sbl.sulmun2yong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// co-payment-settled/co-funding-failed 토픽을 구독하는 독립 consumer 진입점.
// 책임: 장벽 집계(전원 결제 -> 설문 활성화), 환불 팬아웃(CANCEL 적재), 기한 만료 무산 스케쥴러.
@SpringBootApplication
class CoFundingConsumerApplication

fun main(args: Array<String>) {
    runApplication<CoFundingConsumerApplication>(*args)
}
