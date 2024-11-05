package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIEditLog
import com.sbl.sulmun2yong.ai.entity.AIEditLogDocument
import com.sbl.sulmun2yong.ai.exception.AIEditLogNotFoundException
import com.sbl.sulmun2yong.ai.repository.AIEditLogRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AIEditLogAdapter(
    private val aiEditLogRepository: AIEditLogRepository,
) {
    fun saveEditLog(aiEditLog: AIEditLog) =
        aiEditLogRepository.save(
            AIEditLogDocument.from(
                aiEditLog,
            ),
        )

    fun getLatestEditLog(
        surveyId: UUID,
        makerId: UUID,
    ) = aiEditLogRepository
        .findFirstBySurveyIdAndMakerIdOrderByCreatedAtDesc(surveyId, makerId)
        .orElseThrow { AIEditLogNotFoundException() }
        .toDomain()
}
