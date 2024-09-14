package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SurveyRepository :
    MongoRepository<SurveyDocument, UUID>,
    SurveyCustomRepository {
    fun findByStatusAndIsVisibleTrue(
        status: SurveyStatus,
        pageable: Pageable,
    ): Page<SurveyDocument>

    fun existsByIdAndMakerId(
        surveyId: UUID,
        userId: UUID,
    ): Boolean

    fun findByMakerId(makerId: UUID): List<SurveyDocument>
}
