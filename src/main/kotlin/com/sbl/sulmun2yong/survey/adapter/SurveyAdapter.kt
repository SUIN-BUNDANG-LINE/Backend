package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.entity.SurveyEntity
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class SurveyAdapter(
    private val surveyRepository: SurveyRepository,
) {
    fun findSurveysWithPagination(
        size: Int,
        page: Int,
        sortType: SurveySortType,
        isRewardExist: Boolean?,
        isResultOpen: Boolean?,
    ): Page<Survey> {
        val pageRequest = PageRequest.of(page, size, getSurveySort(sortType))
        val surveyEntities =
            surveyRepository.findSurveysWithPagination(
                size = size,
                page = page,
                sortType = sortType,
                isRewardExist = isRewardExist,
                isResultOpen = isResultOpen,
            )
        val surveys = surveyEntities.content.map { it.toDomain() }
        return PageImpl(surveys, pageRequest, surveyEntities.totalElements)
    }

    fun getSurvey(surveyId: UUID) =
        surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }.toDomain()

    private fun getSurveySort(sortType: SurveySortType) =
        when (sortType) {
            SurveySortType.RECENT -> Sort.by("publishedAt").ascending()
            SurveySortType.OLDEST -> Sort.by("publishedAt").descending()
        }

    fun save(survey: Survey) {
        surveyRepository.save(SurveyEntity.from(survey))
    }

    fun getByIdAndMakerId(
        surveyId: UUID,
        makerId: UUID,
    ) = surveyRepository.findByIdAndMakerIdAndIsDeletedFalse(surveyId, makerId).orElseThrow { SurveyNotFoundException() }.toDomain()

    fun getMyPageSurveysInfo(
        makerId: UUID,
        status: SurveyStatus?,
        sortType: MySurveySortType,
    ) = surveyRepository.findSurveysWithResponseCount(makerId, status, sortType)

    fun delete(
        surveyId: UUID,
        makerId: UUID,
    ) {
        val isSuccess = surveyRepository.softDelete(surveyId, makerId)
        if (!isSuccess) throw SurveyNotFoundException()
    }

    fun findFinishTargets(now: Date) = surveyRepository.findFinishTargets(now).map { it.toDomain() }

    fun saveAll(surveys: List<Survey>) {
        surveyRepository.saveAll(surveys.map { SurveyEntity.from(it) })
    }
}
