package com.sbl.sulmun2yong.global.migration

import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.bson.types.Binary
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.nio.ByteBuffer
import java.util.UUID

/** Responses 컬렉션에 surveyId를 넣는 Migration Class */
@ChangeUnit(id = "AddSurveyIdAtResponses", order = "005", author = "hunhui")
class AddSurveyIdAtResponses(
    private val mongoTemplate: MongoTemplate,
    private val surveyRepository: SurveyRepository,
) {
    private val log = LoggerFactory.getLogger(AddSurveyIdAtResponses::class.java)

    @Execution
    fun addSurveyIdAtResponses() {
        val questionIdSurveyIdMap =
            surveyRepository
                .findAll()
                .flatMap { survey ->
                    survey.sections.flatMap { section ->
                        section.questions.map { question ->
                            question.questionId to survey.id
                        }
                    }
                }.toMap()

        val responseDocuments = mongoTemplate.find(Query(), Map::class.java, "responses")

        responseDocuments.forEach { response ->
            val questionIdBinary = response["questionId"] as? Binary
            val questionId = questionIdBinary?.let { binaryToUUID(it) }
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

        log.info("005-AddSurveyIdAtResponses 완료")
    }

    private fun binaryToUUID(binary: Binary): UUID {
        val byteBuffer = ByteBuffer.wrap(binary.data)
        val mostSigBits = byteBuffer.long
        val leastSigBits = byteBuffer.long
        return UUID(mostSigBits, leastSigBits)
    }

    @RollbackExecution
    fun rollback() {
        mongoTemplate.updateMulti(
            Query(Criteria.where("surveyId").exists(true)),
            Update().unset("surveyId"),
            "responses",
        )

        log.warn("005-AddSurveyIdAtResponses 롤백")
    }
}
