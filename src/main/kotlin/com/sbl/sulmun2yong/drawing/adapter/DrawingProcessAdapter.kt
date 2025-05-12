package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.lock.RedissonLock
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DrawingProcessAdapter(
    private val drawingBoardAdapter: DrawingBoardAdapter,
    private val drawingHistoryAdapter: DrawingHistoryAdapter,
    private val surveyAdapter: SurveyAdapter,
) {
    @RedissonLock(key = "'drawingLock:' + #surveyId", leaseTime = 10, waitTime = 5)
    @Transactional
    fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 추첨 가능 여부 조회
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        val drawingResult = drawingBoard.getDrawingResult(selectedNumber)

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

        // 추첨 결과 저장
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
            surveyAdapter.save(surveyAdapter.getSurvey(surveyId).finish())
        }

        return when (drawingResult) {
            is DrawingResult.Winner -> DrawingResultResponse.Winner(drawingResult.rewardName)
            is DrawingResult.NonWinner -> DrawingResultResponse.NonWinner()
        }
    }
}
