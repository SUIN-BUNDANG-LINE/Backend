package com.sbl.sulmun2yong.global.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.bson.types.Binary
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/** Responses 컬렉션에 surveyId를 넣는 Migration Class */
@ChangeUnit(id = "AddSurveyIdAtResponses", order = "002", author = "hunhui")
class AddSurveyIdAtResponses(
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(AddSurveyIdAtResponses::class.java)

    @Execution
    fun addSurveyIdAtResponses() {
        val surveys = mongoTemplate.find(Query(), Map::class.java, "surveys")
        val questionIdSurveyIdMap =
            surveys
                .flatMap { survey ->
                    (survey["sections"] as List<Map<String, Any>>).flatMap { section ->
                        (section["questions"] as List<Map<String, Any>>).map { question ->
                            question["questionId"] to survey["_id"]
                        }
                    }
                }.toMap()

        val responseDocuments = mongoTemplate.find(Query(), Map::class.java, "responses")

        responseDocuments.forEach { response ->
            val questionId = response["questionId"] as? Binary
            if (questionId != null) {
                val surveyId = questionIdSurveyIdMap[questionId]
                if (surveyId != null) {
                    val update = Update().set("surveyId", surveyId)
                    val updateQuery = Query(Criteria.where("_id").`is`(response["_id"]))
                    mongoTemplate.updateFirst(updateQuery, update, "responses")
                } else {
                    log.warn("Response ${response["_id"]}에 해당하는 surveyId를 찾을 수 없습니다.")
                }
            } else {
                log.warn("Response ${response["_id"]}에 questionId가 없습니다.")
            }
        }

        log.info("002-AddSurveyIdAtResponses 완료")
    }

    @RollbackExecution
    fun rollback() {
        mongoTemplate.updateMulti(
            Query(Criteria.where("surveyId").exists(true)),
            Update().unset("surveyId"),
            "responses",
        )

        log.warn("002-AddSurveyIdAtResponses 롤백")
    }
}
