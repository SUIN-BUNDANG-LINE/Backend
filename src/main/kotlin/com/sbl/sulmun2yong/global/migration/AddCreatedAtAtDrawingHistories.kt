package com.sbl.sulmun2yong.global.migration

import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.util.Date

/** RewardHistories 컬렉션에 createdAt을 넣는 Migration Class */
@ChangeUnit(id = "AddCreatedAtAtDrawingHistories", order = "006", author = "hunhui")
class AddCreatedAtAtDrawingHistories(
    private val mongoTemplate: MongoTemplate,
    private val surveyRepository: SurveyRepository,
) {
    private val log = LoggerFactory.getLogger(AddCreatedAtAtDrawingHistories::class.java)

    @Execution
    fun addCreatedAtAtDrawingHistories() {
        val query = Query(Criteria.where("createdAt").`is`(null))
        val now = Date()
        val update = Update().set("createdAt", now)
        mongoTemplate.updateMulti(query, update, "drawingHistories")
        log.info("006-AddCreatedAtAtDrawingHistories 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("006-AddCreatedAtAtDrawingHistories 롤백")
    }
}
