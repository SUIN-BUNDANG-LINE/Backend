package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveyInfoResponse
import com.sbl.sulmun2yong.survey.entity.SurveyEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

class SurveyCustomRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager,
) : SurveyCustomRepository {
    override fun findSurveysWithResponseCount(
        makerId: UUID,
        status: SurveyStatus?,
        sortType: MySurveySortType,
    ): List<MyPageSurveyInfoResponse> {
        val statusCondition = if (status != null) "AND s.status = :status" else ""
        val orderBy = sortType.toOrderBy()

        val jpql =
            """
            SELECT new com.sbl.sulmun2yong.survey.dto.response.MyPageSurveyInfoResponse(
                s.id, s.title, s.thumbnail, s.updatedAt, s.status, s.finishedAt,
                CAST((SELECT COUNT(p) FROM ParticipantEntity p WHERE p.surveyId = s.id) AS int)
            )
            FROM SurveyEntity s
            WHERE s.makerId = :makerId AND s.isDeleted = false $statusCondition
            ORDER BY $orderBy
            """.trimIndent()

        val query = entityManager.createQuery(jpql, MyPageSurveyInfoResponse::class.java)
        query.setParameter("makerId", makerId)
        if (status != null) query.setParameter("status", status)

        return query.resultList
    }

    override fun softDelete(
        surveyId: UUID,
        makerId: UUID,
    ): Boolean {
        val jpql = "UPDATE SurveyEntity s SET s.isDeleted = true WHERE s.id = :surveyId AND s.makerId = :makerId"
        val updatedCount =
            entityManager
                .createQuery(jpql)
                .setParameter("surveyId", surveyId)
                .setParameter("makerId", makerId)
                .executeUpdate()
        return updatedCount > 0
    }

    override fun findSurveysWithPagination(
        size: Int,
        page: Int,
        sortType: SurveySortType,
        isRewardExist: Boolean?,
        isResultOpen: Boolean?,
    ): Page<SurveyEntity> {
        val pageRequest = PageRequest.of(page, size, getSurveySort(sortType))

        val conditions = mutableListOf("s.status = 'IN_PROGRESS'", "s.isVisible = true", "s.isDeleted = false")
        if (isRewardExist == true) conditions.add("s.rewardSettingType <> 'NO_REWARD'")
        if (isResultOpen == true) conditions.add("s.isResultOpen = true")

        val whereClause = conditions.joinToString(" AND ")
        val orderBy = getSurveySortString(sortType)

        val jpql = "SELECT s FROM SurveyEntity s WHERE $whereClause ORDER BY $orderBy"
        val countJpql = "SELECT COUNT(s) FROM SurveyEntity s WHERE $whereClause"

        val query = entityManager.createQuery(jpql, SurveyEntity::class.java)
        query.firstResult = page * size
        query.maxResults = size

        val total = entityManager.createQuery(countJpql, Long::class.java).singleResult

        return PageImpl(query.resultList, pageRequest, total)
    }

    private fun MySurveySortType.toOrderBy() =
        when (this) {
            MySurveySortType.LAST_MODIFIED -> "s.updatedAt DESC"
            MySurveySortType.OLD_MODIFIED -> "s.updatedAt ASC"
            MySurveySortType.TITLE_ASC -> "s.title ASC"
            MySurveySortType.TITLE_DESC -> "s.title DESC"
        }

    private fun getSurveySort(sortType: SurveySortType) =
        when (sortType) {
            SurveySortType.RECENT -> Sort.by("createdAt").descending()
            SurveySortType.OLDEST -> Sort.by("createdAt").ascending()
        }

    private fun getSurveySortString(sortType: SurveySortType) =
        when (sortType) {
            SurveySortType.RECENT -> "s.createdAt DESC"
            SurveySortType.OLDEST -> "s.createdAt ASC"
        }
}
