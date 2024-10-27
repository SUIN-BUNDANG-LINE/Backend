package com.sbl.sulmun2yong.ai.adapter

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@Component
class AIDemoCountRedisAdapter(
    private val redisTemplate: RedisTemplate<String, Int>,
) {
    fun incrementOrCreate(visitorId: String) {
        val key = "demoCount:$visitorId"

        val currentCount = redisTemplate.opsForValue().get(key) ?: 0

        redisTemplate.opsForValue().set(key, currentCount + 1)

        val secondsUntilMidnight = calculateTTL()
        redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS)
    }

    // 자정까지 남은 시간을 계산하는 메서드
    private fun calculateTTL(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().atTime(LocalTime.MIDNIGHT).plusDays(1)
        return ChronoUnit.SECONDS.between(now, midnight)
    }
}
