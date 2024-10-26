package com.sbl.sulmun2yong.ai.entity.redis

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@RedisHash("demoCount")
data class AIDemoCountRedisEntity(
    @Id
    val key: String,
    val count: Int,
) {
    @TimeToLive
    var expiration: Long = calculateTTL()

    companion object {
        // 자정까지 남은 시간(초 단위)을 계산하는 메서드
        private fun calculateTTL(): Long {
            val now = LocalDateTime.now()
            val midnight = now.toLocalDate().atTime(LocalTime.MIDNIGHT).plusDays(1)
            return ChronoUnit.SECONDS.between(now, midnight)
        }
    }
}
