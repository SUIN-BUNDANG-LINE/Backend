package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.adapter.DrawingProcessAdapter
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.FinishedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardAccessException
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DrawingBoardService(
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
    private val drawingBoardAdapter: DrawingBoardAdapter,
    private val drawingProcessAdapter: DrawingProcessAdapter,
) {
    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val surveyStatus = surveyAdapter.getSurvey(surveyId).status
        if (surveyStatus == SurveyStatus.NOT_STARTED || surveyStatus == SurveyStatus.CLOSED) {
            throw InvalidDrawingBoardAccessException()
        }
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        return DrawingBoardResponse.of(drawingBoard)
    }

    fun doDrawing(
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 참가자 및 설문 정보 확인
        val participant = participantAdapter.getByParticipantId(participantId)
        val surveyId = participant.surveyId

        // 설문 상태 확인
        val survey = surveyAdapter.getSurvey(surveyId)
        if (survey.status == SurveyStatus.CLOSED) {
            throw FinishedDrawingException()
        }

        return drawingProcessAdapter.processDrawing(surveyId, participantId, selectedNumber, phoneNumber)
    }
}
