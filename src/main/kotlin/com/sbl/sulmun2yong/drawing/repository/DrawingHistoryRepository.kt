package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingHistoryRepository : MongoRepository<DrawingHistoryDocument, UUID> {
    @Query("{ '\$or': [ { 'participantId': ?0 }, { 'phoneNumber': ?1 } ] }")
    fun findByParticipantIdOrPhoneNumber(
        participantId: UUID,
        phoneNumber: String,
    ): Optional<DrawingHistoryDocument>
}
