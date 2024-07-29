package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingHistoryRepository : MongoRepository<DrawingHistoryDocument, UUID> {
    fun findByParticipantId(id: UUID): Optional<DrawingHistoryDocument>
}
