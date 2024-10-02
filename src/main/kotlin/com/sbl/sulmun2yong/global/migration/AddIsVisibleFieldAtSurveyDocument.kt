package com.sbl.sulmun2yong.global.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/** Surveys 컬렉션의 isVisible이 null인 경우 기본값 true를 넣는 Migration Class */
@ChangeUnit(id = "AddIsVisibleFieldAtSurveyDocument", order = "006", author = "hunhui")
class AddIsVisibleFieldAtSurveyDocument {
    private val log = LoggerFactory.getLogger(AddIsVisibleFieldAtSurveyDocument::class.java)

    @Execution
    fun addIsDeletedField(mongoTemplate: MongoTemplate) {
        val query = Query(Criteria.where("isVisible").`is`(null))
        val update = Update().set("isVisible", true)
        mongoTemplate.updateMulti(query, update, "surveys")
        log.info("006-AddIsVisibleFieldAtSurveyDocument 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("006-AddIsVisibleFieldAtSurveyDocument 롤백")
    }
}
