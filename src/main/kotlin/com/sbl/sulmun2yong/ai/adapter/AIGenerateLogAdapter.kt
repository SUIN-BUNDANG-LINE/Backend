package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIGenerateLog
import com.sbl.sulmun2yong.ai.entity.AIGenerateLogDocument
import com.sbl.sulmun2yong.ai.repository.AIGenerateLogRepository
import org.springframework.stereotype.Component

@Component
class AIGenerateLogAdapter(
    private val aiGenerateLogRepository: AIGenerateLogRepository,
) {
    fun saveGenerateLog(aiGenerateLog: AIGenerateLog) =
        aiGenerateLogRepository.save(
            AIGenerateLogDocument.from(
                aiGenerateLog,
            ),
        )
}
