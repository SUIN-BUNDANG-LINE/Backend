package com.sbl.sulmun2yong.consumer.event

import org.springframework.kafka.support.Acknowledgment

// Kafka Consumer의 ack를 트랜잭션 커밋 후로 지연시키기 위한 이벤트
data class KafkaAckEvent(
    val ack: Acknowledgment,
)
