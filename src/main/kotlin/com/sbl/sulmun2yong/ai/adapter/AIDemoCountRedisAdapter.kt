package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.exception.AIDemoCountLimitException
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
    companion object {
        private const val DEMO_COUNT_KEY_PREFIX = "demoCount"
        private const val DEMO_COUNT_LIMIT = 100
    }

    fun incrementOrCreate(visitorId: String) {
        val key = "$DEMO_COUNT_KEY_PREFIX:$visitorId"

        val currentCount = redisTemplate.opsForValue().get(key) ?: 0
        if (currentCount >= DEMO_COUNT_LIMIT) throw AIDemoCountLimitException()

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
