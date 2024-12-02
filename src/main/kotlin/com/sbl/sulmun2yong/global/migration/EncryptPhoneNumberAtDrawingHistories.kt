package com.sbl.sulmun2yong.global.migration

import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/** DrawingHistories 컬렉션의 phoneNumber를 암호화하는 Migration Class */
@ChangeUnit(id = "EncryptPhoneNumberAtDrawingHistories", order = "008", author = "hunhui")
class EncryptPhoneNumberAtDrawingHistories(
    private val mongoTemplate: MongoTemplate,
    private val encryptionUtils: EncryptionUtils,
) {
    private val log = LoggerFactory.getLogger(EncryptPhoneNumberAtDrawingHistories::class.java)

    @Execution
    fun encryptPhoneNumberAtDrawingHistories() {
        val query = Query()
        val documents = mongoTemplate.find(query, DrawingHistoryDocument::class.java, "drawingHistories")

        documents.forEach { document ->
            val encryptedPhoneNumber = encryptionUtils.encrypt(document.phoneNumber)
            val update = Update().set("phoneNumber", encryptedPhoneNumber)
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").`is`(document.id)), update, "drawingHistories")
        }

        log.info("008-EncryptPhoneNumberAtDrawingHistories 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("008-EncryptPhoneNumberAtDrawingHistories 롤백")
    }
}
