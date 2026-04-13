package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.entity.SurveyEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional
import java.util.UUID

@Repository
interface SurveyRepository :
    JpaRepository<SurveyEntity, UUID>,
    SurveyCustomRepository {
    fun findByStatusAndIsVisibleTrueAndIsDeletedFalse(
        status: SurveyStatus,
        pageable: Pageable,
    ): Page<SurveyEntity>

    fun findByIdAndMakerIdAndIsDeletedFalse(
        id: UUID,
        makerId: UUID,
    ): Optional<SurveyEntity>

    fun findByIdAndIsDeletedFalse(id: UUID): Optional<SurveyEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SurveyEntity s WHERE s.id = :id AND s.isDeleted = false")
    fun findByIdAndIsDeletedFalseWithLock(
        @Param("id") id: UUID,
    ): Optional<SurveyEntity>

    @Query(
        "SELECT s FROM SurveyEntity s WHERE s.finishedAt < :now " +
            "AND s.status IN ('IN_PROGRESS', 'IN_MODIFICATION') AND s.isDeleted = false",
    )
    fun findFinishTargets(
        @Param("now") now: Date,
    ): List<SurveyEntity>
}
