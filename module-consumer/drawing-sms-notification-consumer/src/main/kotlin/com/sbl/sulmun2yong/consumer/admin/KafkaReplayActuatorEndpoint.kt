package com.sbl.sulmun2yong.consumer.admin

import com.sbl.sulmun2yong.global.kafka.consumer.replay.ReplayableConsumer
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
import org.springframework.stereotype.Component

// 운영자/실험용 — Kafka consumer 리플레이 트리거.
// 노출: management 포트 (POST /management/kafkaReplay/{mode})
// 운영 적용 시 Spring Security 인증/인가 적용 필수.
@Component
@Endpoint(id = "kafkaReplay")
class KafkaReplayActuatorEndpoint(
    consumers: List<ReplayableConsumer>,
) {
    private val log = LoggerFactory.getLogger(KafkaReplayActuatorEndpoint::class.java)

    // name → ReplayableConsumer 로 정적 매핑 (Spring 컬렉션 주입)
    private val consumersByName: Map<String, ReplayableConsumer> = consumers.associateBy { it.name }

    @WriteOperation
    fun replay(
        @Selector mode: String,
        target: String?,
        epochMillis: Long?,
        partition: Int?,
        offset: Long?,
    ): Map<String, Any?> {
        log.info(
            "kafkaReplay 호출: mode={}, target={}, epochMillis={}, partition={}, offset={}",
            mode,
            target,
            epochMillis,
            partition,
            offset,
        )

        val resolved = resolveConsumer(target) ?: return targetError(target)
        val consumer = resolved

        return when (mode) {
            "beginning" -> {
                consumer.fromBeginning()
                mapOf("status" to "dispatched", "mode" to "beginning", "target" to consumer.name)
            }
            "timestamp" -> {
                require(epochMillis != null) { "epochMillis 필수" }
                consumer.fromTimestamp(epochMillis)
                mapOf(
                    "status" to "dispatched",
                    "mode" to "timestamp",
                    "target" to consumer.name,
                    "epochMillis" to epochMillis,
                )
            }
            "offset" -> {
                require(partition != null && offset != null) { "partition, offset 필수" }
                consumer.fromOffset(partition, offset)
                mapOf(
                    "status" to "dispatched",
                    "mode" to "offset",
                    "target" to consumer.name,
                    "partition" to partition,
                    "offset" to offset,
                )
            }
            else -> mapOf("status" to "error", "message" to "unknown mode: $mode (beginning|timestamp|offset)")
        }
    }

    private fun resolveConsumer(target: String?): ReplayableConsumer? {
        if (target != null) {
            return consumersByName[target]
        }
        // target 미지정 시 등록된 컨슈머가 정확히 1개일 때만 자동 선택
        return consumersByName.values.singleOrNull()
    }

    private fun targetError(target: String?): Map<String, Any?> =
        if (target == null) {
            mapOf(
                "status" to "error",
                "message" to
                    "target 필수 (등록된 consumer ${consumersByName.size}개): ${consumersByName.keys}",
            )
        } else {
            mapOf(
                "status" to "error",
                "message" to "unknown target: $target (등록된 consumer: ${consumersByName.keys})",
            )
        }
}
