package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.entity.redis.AIDemoCountRedisEntity
import com.sbl.sulmun2yong.ai.exception.AIDemoCountLimitException
import com.sbl.sulmun2yong.ai.repository.redis.AIDemoCountRedisRepository
import org.springframework.stereotype.Component

@Component
class AIDemoCountRedisAdapter(
    private val aiDemoCountRedisRepository: AIDemoCountRedisRepository,
) {
    companion object {
        private const val MAX_COUNT = 100
    }

    fun incrementOrCreate(visitorId: String) {
        val key = makeKey(visitorId)

        // 현재 count 값을 확인하고, maxCount 초과할 경우 예외 발생
        val currentCount = aiDemoCountRedisRepository.findById(key).map { it.count }.orElse(0)
        if (currentCount >= MAX_COUNT) throw AIDemoCountLimitException()

        // count 증가 또는 새 엔티티 생성
        val entity = AIDemoCountRedisEntity(key, currentCount + 1)
        aiDemoCountRedisRepository.save(entity)
    }

    private fun makeKey(visitorId: String) = "demoCount:$visitorId"
}
