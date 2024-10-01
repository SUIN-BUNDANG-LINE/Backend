package com.sbl.sulmun2yong.global.migration

import com.sbl.sulmun2yong.global.error.GlobalExceptionHandler
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

/** 최초에 컬렉션들을 생성하는 Migration Class */
@ChangeUnit(id = "InitialMongoDBSetUp", order = "001", author = "hunhui")
class InitialMongoDBSetUp {
    companion object {
        private const val SURVEY_COLLECTION = "surveys"
        private const val DRAWING_BOARD_COLLECTION = "drawingBoards"
        private const val DRAWING_HISTORY_COLLECTION = "drawingHistories"
        private const val PARTICIPANT_COLLECTION = "participants"
        private const val RESPONSE_COLLECTION = "responses"
        private const val USER_COLLECTION = "users"
        val COLLECTIONS =
            listOf(
                SURVEY_COLLECTION,
                DRAWING_BOARD_COLLECTION,
                DRAWING_HISTORY_COLLECTION,
                PARTICIPANT_COLLECTION,
                RESPONSE_COLLECTION,
                USER_COLLECTION,
            )
    }

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @Execution
    fun initialSetup(mongoTemplate: MongoTemplate) {
        val collectionNames = mongoTemplate.db.listCollectionNames().toList()
        for (collection in COLLECTIONS) {
            if (!collectionNames.contains(collection)) {
                mongoTemplate.db.createCollection(collection)
            }
        }
        log.info("001-InitialMongoDBSetUp 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("001-InitialMongoDBSetUp 롤백")
    }
}
