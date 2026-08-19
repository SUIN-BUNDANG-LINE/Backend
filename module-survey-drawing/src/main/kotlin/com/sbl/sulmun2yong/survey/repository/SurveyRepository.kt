package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.Survey
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SurveyRepository :
    JpaRepository<Survey, UUID>,
    SurveyCustomRepository {
    fun findByIdAndMakerIdAndIsDeletedFalse(
        id: UUID,
        makerId: UUID,
    ): Optional<Survey>

    fun findByIdAndIsDeletedFalse(id: UUID): Optional<Survey>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Survey s WHERE s.id = :id AND s.isDeleted = false")
    fun findByIdAndIsDeletedFalseWithLock(
        @Param("id") id: UUID,
    ): Optional<Survey>
}
