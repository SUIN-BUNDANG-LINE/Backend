package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.entity.AIEditLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AIEditLogRepository : JpaRepository<AIEditLogEntity, UUID> {
    fun findFirstBySurveyIdAndMakerIdOrderByCreatedAtDesc(
        surveyId: UUID,
        makerId: UUID,
    ): Optional<AIEditLogEntity>
}
