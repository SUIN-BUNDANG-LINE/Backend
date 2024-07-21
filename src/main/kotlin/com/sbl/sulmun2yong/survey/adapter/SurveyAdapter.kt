package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.Reward
import com.sbl.sulmun2yong.survey.domain.Section
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SurveyAdapter(private val surveyRepository: SurveyRepository) {
    fun findSurveysWithPagination(
        size: Int,
        page: Int,
        sortType: SurveySortType,
        isAsc: Boolean,
    ): Page<Survey> {
        val pageRequest = PageRequest.of(page, size, getSurveySort(sortType, isAsc))
        val surveyDocuments = surveyRepository.findByStatus(SurveyStatus.IN_PROGRESS, pageRequest)
        val surveys = surveyDocuments.content.map { it.toDomain() }
        return PageImpl(surveys, pageRequest, surveyDocuments.totalElements)
    }

    fun findSurvey(surveyId: UUID) = surveyRepository.findById(surveyId).orElseThrow { SurveyNotFoundException() }.toDomain()

    private fun getSurveySort(
        sortType: SurveySortType,
        isAsc: Boolean,
    ) = when (sortType) {
        SurveySortType.RECENT -> {
            if (isAsc) {
                Sort.by("createdAt").ascending()
            } else {
                Sort.by("createdAt").descending()
            }
        }
    }

    private fun SurveyDocument.toDomain() =
        Survey(
            id = this.id,
            title = this.title,
            description = this.description,
            thumbnail = this.thumbnail,
            finishedAt = this.finishedAt,
            // TODO: 실제 publishedAt을 넣기
            publishedAt = this.createdAt,
            status = this.status,
            finishMessage = this.finishMessage,
            targetParticipantCount = this.targetParticipants,
            rewards = this.rewards.map { it.toDomain() },
            // TODO: 실제 sections를 넣기
            sections = listOf(Section.create()),
        )

    private fun SurveyDocument.RewardSubDocument.toDomain() =
        Reward(
            id = this.rewardId,
            name = this.name,
            category = this.category,
            count = this.count,
        )
}
