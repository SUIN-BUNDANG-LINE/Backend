package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.MySurveySortType
import com.sbl.sulmun2yong.survey.dto.request.SurveySortType
import com.sbl.sulmun2yong.survey.dto.response.MyPageSurveysResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyListResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import com.sbl.sulmun2yong.survey.dto.response.SurveyProgressInfoResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyAccessException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SurveyInfoService(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    fun getSurveysWithPagination(
        size: Int,
        page: Int,
        sortType: SurveySortType,
        isRewardExist: Boolean?,
        isResultOpen: Boolean?,
    ): SurveyListResponse {
        val pageRequest = PageRequest.of(page, size, getSurveySort(sortType))
        val surveyEntities =
            surveyRepository.findSurveysWithPagination(
                size = size,
                page = page,
                sortType = sortType,
                isRewardExist = isRewardExist,
                isResultOpen = isResultOpen,
            )
        return SurveyListResponse.of(surveyEntities.totalPages, surveyEntities.content)
    }

    // OSIV=false 환경에서 rewardSetting(@Transient → rewardEntities lazy)·
    // drawingBoard.selectedTicketCount(ticketEntities lazy) 접근이 LazyInitializationException을 던지므로
    // 단일 read 트랜잭션으로 묶어 세션을 유지한다.
    @Transactional(readOnly = true)
    fun getSurveyInfo(surveyId: UUID): SurveyInfoResponse {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (survey.status == SurveyStatus.NOT_STARTED) throw InvalidSurveyAccessException()
        val selectedTicketCount =
            if (survey.rewardSetting.isImmediateDraw) {
                drawingBoardRepository
                    .findBySurveyIdWithTickets(surveyId)
                    .orElseThrow { InvalidDrawingBoardException() }
                    .selectedTicketCount
            } else {
                null
            }
        return SurveyInfoResponse.of(survey, selectedTicketCount)
    }

    fun getSurveyProgressInfo(surveyId: UUID): SurveyProgressInfoResponse? {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (survey.status != SurveyStatus.IN_PROGRESS) throw InvalidSurveyAccessException()
        return SurveyProgressInfoResponse.of(survey)
    }

    fun getMyPageSurveys(
        makerId: UUID,
        status: SurveyStatus?,
        sortType: MySurveySortType,
    ): MyPageSurveysResponse {
        val myPageSurveysInfoResponse = surveyRepository.findSurveysWithResponseCount(makerId, status, sortType)
        return MyPageSurveysResponse(myPageSurveysInfoResponse)
    }

    fun getSurveyMakeInfo(surveyId: UUID): SurveyMakeInfoResponse {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        return SurveyMakeInfoResponse.of(survey)
    }

    private fun getSurveySort(sortType: SurveySortType) =
        when (sortType) {
            SurveySortType.RECENT -> Sort.by("publishedAt").ascending()
            SurveySortType.OLDEST -> Sort.by("publishedAt").descending()
        }
}
