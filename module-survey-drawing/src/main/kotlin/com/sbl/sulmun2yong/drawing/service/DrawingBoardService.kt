package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.FinishedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardAccessException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.service.strategy.DrawMode
import com.sbl.sulmun2yong.drawing.service.strategy.DrawingStrategy
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.InvalidParticipantException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DrawingBoardService(
    private val surveyRepository: SurveyRepository,
    private val participantRepository: ParticipantRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    strategies: List<DrawingStrategy>,
) {
    // 5개 전략(NO_LOCK·SERIALIZABLE·OPTIMISTIC_RETRY·SYNCHRONIZED·REDISSON)을 모드로 색인
    private val strategyByMode: Map<DrawMode, DrawingStrategy> = strategies.associateBy { it.mode }

    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val surveyStatus =
            surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }.status
        if (surveyStatus == SurveyStatus.NOT_STARTED || surveyStatus == SurveyStatus.CLOSED) {
            throw InvalidDrawingBoardAccessException()
        }
        val drawingBoard = drawingBoardRepository.findBySurveyId(surveyId).orElseThrow { InvalidDrawingBoardException() }
        return DrawingBoardResponse.of(drawingBoard)
    }

    /** 공통 검증(참가자 해석·설문 종료 여부) 후 지정된 전략으로 추첨을 위임한다. 운영 기본은 REDISSON. */
    fun doDrawing(
        mode: DrawMode,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val participant = participantRepository.findById(participantId).orElseThrow { InvalidParticipantException() }
        val surveyId = participant.surveyId

        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (survey.status == SurveyStatus.CLOSED) {
            throw FinishedDrawingException()
        }

        return strategyByMode.getValue(mode).processDrawing(surveyId, participantId, selectedNumber, phoneNumber)
    }
}
