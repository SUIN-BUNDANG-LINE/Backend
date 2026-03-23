package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIGenerateLog
import com.sbl.sulmun2yong.ai.entity.AIGenerateLogEntity
import com.sbl.sulmun2yong.ai.repository.AIGenerateLogRepository
import org.springframework.stereotype.Component

@Component
class AIGenerateLogAdapter(
    private val aiGenerateLogRepository: AIGenerateLogRepository,
) {
    fun saveGenerateLog(aiGenerateLog: AIGenerateLog) = aiGenerateLogRepository.save(AIGenerateLogEntity.from(aiGenerateLog))
}
