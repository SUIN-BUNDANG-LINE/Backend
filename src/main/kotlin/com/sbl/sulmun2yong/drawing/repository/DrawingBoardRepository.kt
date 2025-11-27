package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingBoardEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingBoardRepository : JpaRepository<DrawingBoardEntity, UUID> {
    fun findBySurveyId(surveyId: UUID): Optional<DrawingBoardEntity>

    fun deleteBySurveyId(surveyId: UUID)
}
