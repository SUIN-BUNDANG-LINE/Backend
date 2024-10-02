package com.sbl.sulmun2yong.global.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.ZoneId
import java.util.Date

/** Surveys 컬렉션의 finishedAt의 분 단위 이하를 0으로 수정하는 Migration Class */
@ChangeUnit(id = "UpdateFinishedAtAtSurvey", order = "004", author = "hunhui")
class UpdateFinishedAtAtSurvey(
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(UpdateFinishedAtAtSurvey::class.java)

    @Execution
    fun updateFinishedAtAtSurvey() {
        val surveys = mongoTemplate.find(Query(), Map::class.java, "surveys")

        surveys.forEach { survey ->
            survey["finishedAt"]?.let {
                val updatedFinishedAt =
                    (it as Date)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .toInstant()

                val update = Update().set("finishedAt", updatedFinishedAt)

                val updateQuery = Query(Criteria.where("_id").`is`(survey["_id"]))
                mongoTemplate.updateFirst(updateQuery, update, "surveys")
            }
        }
        log.info("004-UpdateFinishedAtAtSurvey 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("004-UpdateFinishedAtAtSurvey 롤백")
    }
}
