package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingBoardDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingBoardRepository : MongoRepository<DrawingBoardDocument, UUID> {
    fun findBySurveyId(surveyId: UUID): Optional<DrawingBoardDocument>
}
