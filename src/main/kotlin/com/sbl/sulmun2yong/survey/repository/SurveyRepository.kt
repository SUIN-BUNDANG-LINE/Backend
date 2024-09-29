package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional
import java.util.UUID

@Repository
interface SurveyRepository :
    MongoRepository<SurveyDocument, UUID>,
    SurveyCustomRepository {
    fun findByStatusAndIsVisibleTrueAndIsDeletedFalse(
        status: SurveyStatus,
        pageable: Pageable,
    ): Page<SurveyDocument>

    fun findByIdAndMakerIdAndIsDeletedFalse(
        id: UUID,
        makerId: UUID,
    ): Optional<SurveyDocument>

    fun findByIdAndIsDeletedFalse(id: UUID): Optional<SurveyDocument>

    @Query("{ 'finishedAt': { \$lt: ?0 }, 'status': { \$in: ['IN_PROGRESS', 'IN_MODIFICATION'] }, 'isDeleted': false }")
    fun findFinishTargets(now: Date): List<SurveyDocument>
}
