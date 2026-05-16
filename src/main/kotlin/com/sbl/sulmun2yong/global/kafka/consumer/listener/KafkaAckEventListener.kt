package com.sbl.sulmun2yong.global.kafka.consumer.listener

import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class KafkaAckEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: KafkaAckEvent) {
        event.ack.acknowledge()
    }
}
