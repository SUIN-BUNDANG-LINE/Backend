package com.sbl.sulmun2yong.survey.adapter

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
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
class SurveyAdapter(
    private val surveyRepository: SurveyRepository,
) {
    fun findSurveysWithPagination(
        size: Int,
        page: Int,
        sortType: SurveySortType,
        isAsc: Boolean,
    ): Page<Survey> {
        val pageRequest = PageRequest.of(page, size, getSurveySort(sortType, isAsc))
        val surveyDocuments = surveyRepository.findByStatusAndIsVisibleTrueAndIsDeletedFalse(SurveyStatus.IN_PROGRESS, pageRequest)
        val surveys = surveyDocuments.content.map { it.toDomain() }
        return PageImpl(surveys, pageRequest, surveyDocuments.totalElements)
    }

    fun getSurvey(surveyId: UUID) =
        surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }.toDomain()

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

    fun save(survey: Survey) {
        val previousSurveyDocument = surveyRepository.findByIdAndIsDeletedFalse(survey.id)
        val surveyDocument = SurveyDocument.from(survey)
        // 기존 설문을 업데이트하는 경우, createdAt을 유지
        if (previousSurveyDocument.isPresent) surveyDocument.createdAt = previousSurveyDocument.get().createdAt
        surveyRepository.save(surveyDocument)
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
}
