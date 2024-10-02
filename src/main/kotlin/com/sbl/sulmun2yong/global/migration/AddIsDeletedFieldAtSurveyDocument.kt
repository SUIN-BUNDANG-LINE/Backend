package com.sbl.sulmun2yong.global.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/** Surveys 컬렉션의 isDelete가 null인 경우 기본값 false를 넣는 Migration Class */
@ChangeUnit(id = "AddIsDeletedFieldAtSurveyDocument", order = "001", author = "hunhui")
class AddIsDeletedFieldAtSurveyDocument {
    private val log = LoggerFactory.getLogger(AddIsDeletedFieldAtSurveyDocument::class.java)

    @Execution
    fun addIsDeletedField(mongoTemplate: MongoTemplate) {
        val query = Query(Criteria.where("isDeleted").`is`(null))
        val update = Update().set("isDeleted", false)
        mongoTemplate.updateMulti(query, update, "surveys")
        log.info("001-AddIsDeletedFieldAtSurveyDocument 완료")
    }

    @RollbackExecution
    fun rollback() {
        log.warn("001-AddIsDeletedFieldAtSurveyDocument 롤백")
    }
}
