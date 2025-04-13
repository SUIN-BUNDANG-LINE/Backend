package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.adapter.DrawingHistoryAdapter
import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.FinishedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardAccessException
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.lock.RedissonLock
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DrawingBoardService(
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
    private val drawingBoardAdapter: DrawingBoardAdapter,
    private val drawingHistoryAdapter: DrawingHistoryAdapter,
) {
    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val surveyStatus = surveyAdapter.getSurvey(surveyId).status
        if (surveyStatus == SurveyStatus.NOT_STARTED || surveyStatus == SurveyStatus.CLOSED) {
            throw InvalidDrawingBoardAccessException()
        }
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        return DrawingBoardResponse.of(drawingBoard)
    }

    /**
     * surveyId를 동적으로 조회한 후 락 키를 생성합니다.
     */
    @RedissonLock(key = "@drawingLockKeyResolver.getLockKey(#participantId, #selectedNumber)", leaseTime = 10)
    @Transactional
    fun doDrawing(
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 참가자 및 설문 정보 확인
        val participant = participantAdapter.getByParticipantId(participantId)
        val surveyId = participant.surveyId

        // 이미 추첨 참여했는지 검증
        val phoneNumberData = PhoneNumber.createWithNonNullable(phoneNumber)
        val drawingHistory =
            drawingHistoryAdapter.findBySurveyIdAndParticipantIdOrPhoneNumber(
                surveyId,
                participantId,
                phoneNumberData,
            )
        if (drawingHistory != null) {
            throw AlreadyParticipatedDrawingException()
        }
        // 설문 상태 확인
        val survey = surveyAdapter.getSurvey(surveyId)
        if (survey.status == SurveyStatus.CLOSED) {
            throw FinishedDrawingException()
        }

        // 추첨 처리
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        val drawingResult = drawingBoard.getDrawingResult(selectedNumber)
        val changedDrawingBoard = drawingResult.changedDrawingBoard
        drawingBoardAdapter.save(changedDrawingBoard)
        drawingHistoryAdapter.insert(
            DrawingHistory.create(
                participantId = participantId,
                phoneNumber = phoneNumberData,
                surveyId = surveyId,
                selectedTicketIndex = selectedNumber,
                ticket = changedDrawingBoard.tickets[selectedNumber],
            ),
        )
        // 보드에 남은 티켓이 없으면 설문 종료 처리
        if (changedDrawingBoard.tickets.size - changedDrawingBoard.selectedTicketCount <= 0) {
            surveyAdapter.save(survey.finish())
        }

        return when (drawingResult) {
            is DrawingResult.Winner -> DrawingResultResponse.Winner(drawingResult.rewardName)
            is DrawingResult.NonWinner -> DrawingResultResponse.NonWinner()
        }
    }
}
