package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.entity.DrawingBoardDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
interface DrawingRepository : MongoRepository<DrawingBoardDocument, UUID> {
    fun save(surveyId: UUID) {
    }

    fun getDrawingBoard(surveyId: String) {
    }
}
