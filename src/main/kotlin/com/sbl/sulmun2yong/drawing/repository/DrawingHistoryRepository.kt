package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.dto.DrawingHistoryDTOGroupedBySurveyId
import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingHistoryRepository : MongoRepository<DrawingHistoryDocument, UUID> {
    @Query(
        value = "{ '\$and': [ { 'surveyId': ?0 }, { '\$or': [ { 'participantId': ?1 }, { 'phoneNumber': ?2 } ] } ] }",
    )
    fun findBySurveyIdAndParticipantIdOrPhoneNumber(
        surveyId: UUID,
        participantId: UUID,
        phoneNumber: String,
    ): Optional<DrawingHistoryDocument>

    @Aggregation(
        pipeline = [
            "{ '\$match': { 'surveyId': ?0 } }",
            "{ '\$group': { '_id': '\$surveyId', 'count': { '\$sum': 1 }, 'items': { '\$push': '\$\$ROOT' } } }",
        ],
    )
    fun findBySurveyId(surveyId: UUID): Optional<DrawingHistoryDTOGroupedBySurveyId>

    @Aggregation(
        pipeline = [
            "{ '\$match': { 'ticket._class': 'com.sbl.sulmun2yong.drawing.domain.ticket.Ticket\$Winning', 'surveyId': ?0 } }",
            "{ '\$group': { '_id': '\$surveyId', 'count': { '\$sum': 1 }, 'items': { '\$push': '\$\$ROOT' } } }",
        ],
    )
    fun findBySurveyIdForWinner(surveyId: UUID): Optional<DrawingHistoryDTOGroupedBySurveyId>

    @Aggregation(
        pipeline = [
            "{ '\$group': { '_id': '\$surveyId', 'count': { '\$sum': 1 }, 'items': { '\$push': '$\$ROOT' } } }",
        ],
    )
    fun findGroupedSurveyId(): List<DrawingHistoryDTOGroupedBySurveyId>

    @Aggregation(
        pipeline = [
            "{ '\$match': { 'ticket._class': 'com.sbl.sulmun2yong.drawing.domain.ticket.Ticket\$Winning' } }",
            "{ '\$group': { '_id': '\$surveyId', 'count': { '\$sum': 1 }, 'items': { '\$push': '\$\$ROOT' } } }",
        ],
    )
    fun findGroupedBySurveyIdForWinner(): List<DrawingHistoryDTOGroupedBySurveyId>
}
