package com.sbl.sulmun2yong.consumer.repository

import com.sbl.sulmun2yong.survey.entity.SurveyEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SurveyConsumerRepository : JpaRepository<SurveyEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SurveyEntity s WHERE s.id = :id AND s.isDeleted = false")
    fun findByIdAndIsDeletedFalseWithLock(
        @Param("id") id: UUID,
    ): Optional<SurveyEntity>
}
