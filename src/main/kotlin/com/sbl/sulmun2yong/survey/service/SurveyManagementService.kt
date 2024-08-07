package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.dto.SurveyCreateResponse
import com.sbl.sulmun2yong.survey.dto.SurveySaveRequest
import com.sbl.sulmun2yong.survey.dto.SurveySaveResponse
import org.springframework.stereotype.Service

// TODO: 추후에 패키지 구조를 변경하여 Service가 특정 도메인이 아닌 요청에 종속되도록 하기
@Service
class SurveyManagementService(
    private val surveyAdapter: SurveyAdapter,
) {
    fun createSurvey(): SurveyCreateResponse {
        val survey = Survey.create()
        surveyAdapter.save(survey)
        return SurveyCreateResponse(surveyId = survey.id)
    }

    fun saveSurvey(surveyCreateRequest: SurveySaveRequest): SurveySaveResponse? {
        val sectionIds = SectionIds.from(surveyCreateRequest.sections.map { SectionId.Standard(it.id) })
        val rewards = surveyCreateRequest.rewards.map { it.toSurveyDomain() }
        val survey =
            with(surveyCreateRequest) {
                Survey(
                    id = surveyCreateRequest.id,
                    title = this.title,
                    description = this.description,
                    thumbnail = this.thumbnail,
                    publishedAt = this.publishedAt,
                    finishedAt = this.finishedAt,
                    status = this.status,
                    finishMessage = this.finishMessage,
                    targetParticipantCount = surveyCreateRequest.targetParticipantCount,
                    rewards = rewards,
                    sections = this.sections.map { it.toDomain(sectionIds) },
                )
            }
        surveyAdapter.save(survey)

        return SurveySaveResponse(surveyId = survey.id)
    }
}
