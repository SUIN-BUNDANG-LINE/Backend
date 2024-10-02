package com.sbl.sulmun2yong.global.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/** Surveys 컬렉션에 rewardSettingType을 넣는 Migration Class */
@ChangeUnit(id = "AddRewardSettingTypeAtSurvey", order = "003", author = "hunhui")
class AddRewardSettingTypeAtSurvey(
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(AddRewardSettingTypeAtSurvey::class.java)

    @Execution
    fun addRewardSettingTypeAtSurvey() {
        // 조건 1: finishedAt이 null이면 rewardSettingType은 NO_REWARD
        val queryNoReward = Query(Criteria.where("finishedAt").exists(false))
        val updateNoReward = Update().set("rewardSettingType", "NO_REWARD")
        mongoTemplate.updateMulti(queryNoReward, updateNoReward, "surveys")

        // 조건 2: finishedAt이 존재하고 targetParticipantCount가 null이면 SELF_MANAGEMENT
        val querySelfManagement =
            Query(
                Criteria().andOperator(
                    Criteria.where("finishedAt").exists(true),
                    Criteria.where("targetParticipantCount").exists(false),
                ),
            )
        val updateSelfManagement = Update().set("rewardSettingType", "SELF_MANAGEMENT")
        mongoTemplate.updateMulti(querySelfManagement, updateSelfManagement, "surveys")

        // 조건 3: 나머지 경우는 IMMEDIATE_DRAW
        val queryImmediateDraw =
            Query(
                Criteria().andOperator(
                    Criteria.where("finishedAt").exists(true),
                    Criteria.where("targetParticipantCount").exists(true),
                ),
            )
        val updateImmediateDraw = Update().set("rewardSettingType", "IMMEDIATE_DRAW")
        mongoTemplate.updateMulti(queryImmediateDraw, updateImmediateDraw, "surveys")

        log.info("003-AddRewardSettingTypeAtSurvey 완료")
    }

    @RollbackExecution
    fun rollback() {
        val query = Query(Criteria.where("rewardSettingType").exists(true))
        val update = Update().unset("rewardSettingType")
        mongoTemplate.updateMulti(query, update, "surveys")

        log.warn("003-AddRewardSettingTypeAtSurvey 롤백")
    }
}
