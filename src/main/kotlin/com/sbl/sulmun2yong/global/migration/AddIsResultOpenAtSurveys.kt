package com.sbl.sulmun2yong.global.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/** Surveys 컬렉션에 isResultOpen을 넣는 Migration Class */
@ChangeUnit(id = "AddIsResultOpenAtSurveys", order = "007", author = "hunhui")
class AddIsResultOpenAtSurveys(
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(AddIsResultOpenAtSurveys::class.java)

    @Execution
    fun addIsResultOpenAtSurveys() {
        val query = Query(Criteria.where("isResultOpen").`is`(null))
        val update = Update().set("isResultOpen", false)
        mongoTemplate.updateMulti(query, update, "surveys")
        log.info("007-AddIsResultOpenAtSurveys 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("007-AddIsResultOpenAtSurveys 롤백")
    }
}
