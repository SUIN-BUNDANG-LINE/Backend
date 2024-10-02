package com.sbl.sulmun2yong.global.migration

import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.ZoneId

/** Surveys 컬렉션의 finishedAt의 분 단위 이하를 0으로 수정하는 Migration Class */
@ChangeUnit(id = "UpdateFinishedAtAtSurvey", order = "004", author = "hunhui")
class UpdateFinishedAtAtSurvey(
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(UpdateFinishedAtAtSurvey::class.java)

    @Execution
    fun updateFinishedAtAtSurvey() {
        // finishedAt 필드가 존재하는 도큐먼트를 모두 조회
        val query = Query(Criteria.where("finishedAt").exists(true))
        val surveys = mongoTemplate.find(query, SurveyDocument::class.java)

        surveys.forEach { survey ->
            survey.finishedAt?.let {
                // finishedAt 시간을 분 이하 단위(초, 나노초) 제거
                val updatedFinishedAt =
                    it
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .toInstant()

                // 업데이트 내용을 설정
                val update = Update().set("finishedAt", updatedFinishedAt)

                // id 기준으로 해당 도큐먼트 업데이트, id 필드 확인 필요
                val updateQuery = Query(Criteria.where("_id").`is`(survey.id)) // MongoDB에서 _id를 사용
                val result = mongoTemplate.updateFirst(updateQuery, update, "surveys")

                // 업데이트 결과 확인
                if (result.modifiedCount == 0L) {
                    log.warn("Survey ${survey.id} 업데이트 실패")
                } else {
                    log.info("Survey ${survey.id} 업데이트 성공")
                }
            }
        }
        log.info("004-UpdateFinishedAtAtSurvey 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("004-UpdateFinishedAtAtSurvey 롤백")
    }
}
