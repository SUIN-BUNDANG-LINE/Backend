package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveyInfoResponse
import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.util.UUID

class SurveyCustomRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : SurveyCustomRepository {
    override fun findSurveysWithResponseCount(
        makerId: UUID,
        status: SurveyStatus?,
        sortType: MySurveySortType,
    ): List<MyPageSurveyInfoResponse> {
        val matchStage =
            Aggregation.match(
                Criteria.where("makerId").`is`(makerId).and("isDeleted").`is`(false).apply {
                    status?.let { and("status").`is`(it) }
                },
            )

        val lookupStage = Aggregation.lookup("participants", "_id", "surveyId", "participantDocs")

        val projectStage =
            Aggregation
                .project("_id", "title", "thumbnail", "updatedAt", "status", "finishedAt")
                .andExpression("size(participantDocs)")
                .`as`("responseCount")

        val sortStage = Aggregation.sort(sortType.toSort())

        val aggregation = Aggregation.newAggregation(matchStage, lookupStage, projectStage, sortStage)

        val results = mongoTemplate.aggregate(aggregation, "surveys", MyPageSurveyInfoResponse::class.java)
        return results.mappedResults
    }

    override fun softDelete(
        surveyId: UUID,
        makerId: UUID,
    ): Boolean {
        val query =
            Query(
                Criteria
                    .where("_id")
                    .`is`(surveyId)
                    .and("makerId")
                    .`is`(makerId),
            )
        val update = Update().set("isDeleted", true)

        val result = mongoTemplate.updateFirst(query, update, SurveyDocument::class.java)

        // 변경된 문서가 있을 경우 true, 없을 경우 false 반환
        return result.modifiedCount > 0
    }

    private fun MySurveySortType.toSort() =
        when (this) {
            MySurveySortType.LAST_MODIFIED -> Sort.by(Sort.Direction.DESC, "updatedAt")
            MySurveySortType.OLD_MODIFIED -> Sort.by(Sort.Direction.ASC, "updatedAt")
            MySurveySortType.TITLE_ASC -> Sort.by(Sort.Direction.ASC, "title")
            MySurveySortType.TITLE_DESC -> Sort.by(Sort.Direction.DESC, "title")
        }
}
