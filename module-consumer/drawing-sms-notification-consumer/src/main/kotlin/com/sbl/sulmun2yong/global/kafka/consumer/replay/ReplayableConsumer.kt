package com.sbl.sulmun2yong.global.kafka.consumer.replay

// 운영자/액추에이터에서 임의 리플레이를 트리거할 수 있는 Kafka consumer 추상화.
// 구체 Listener에 endpoint가 결합되지 않도록 분리 (DIP).
interface ReplayableConsumer {
    val name: String

    fun fromBeginning()

    fun fromTimestamp(epochMillis: Long)

    fun fromOffset(
        partition: Int,
        offset: Long,
    )
}
