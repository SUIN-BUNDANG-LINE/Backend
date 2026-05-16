package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingBoardRepository : JpaRepository<DrawingBoard, UUID> {
    fun findBySurveyId(surveyId: UUID): Optional<DrawingBoard>

    fun deleteBySurveyId(surveyId: UUID)
}
