package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.adapter.DrawingHistoryAdapter
import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.domain.Reward
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.dto.response.NonWinnerDrawingResultResponse
import com.sbl.sulmun2yong.drawing.dto.response.WinnerDrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID
import com.sbl.sulmun2yong.survey.domain.Reward as SurveyReward

@Service
// TODO : mongoDB 트랜잭션 테스트 필요
@Transactional
class DrawingBoardService(
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
    private val drawingBoardAdapter: DrawingBoardAdapter,
    private val drawingHistoryAdapter: DrawingHistoryAdapter,
) {
    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        return DrawingBoardResponse.of(drawingBoard)
    }

    fun doDrawing(
        participantId: UUID,
        selectedNumber: Int,
    ): DrawingResultResponse {
        // 유효성 검증
        // 참가했는가
        val participant = participantAdapter.getParticipant(participantId)
        // 추첨 기록이 있는가
        val drawingHistory = drawingHistoryAdapter.findByParticipantId(participantId)
        if (drawingHistory != null) {
            throw AlreadyParticipatedDrawingException()
        }
        // 설문이 종료되었는가
        val surveyId = participant.surveyId
        val survey = surveyAdapter.getSurvey(surveyId)
        if (Date().after(survey.finishedAt)) {
            throw InvalidDrawingException()
        }

        // 추첨 보드 가져오기
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)

        // 뽑기
        val drawingMachine = DrawingMachine(drawingBoard, selectedNumber)
        drawingMachine.insertQuarter()
        drawingMachine.selectTicket()

        val drawingResultResponse =
            if (drawingMachine.openTicketAndCheckIsWon()) {
                WinnerDrawingResultResponse.create(drawingMachine.getRewardName())
            } else {
                NonWinnerDrawingResultResponse.create()
            }

        // 보드 업데이트
        drawingBoardAdapter.save(drawingBoard)
        // 추첨 기록 저장
        drawingHistoryAdapter.save(
            DrawingHistory.create(
                participantId = participantId,
                drawingBoardId = drawingBoard.id,
                selectedTicketIndex = selectedNumber,
                ticket = drawingBoard.tickets[selectedNumber],
            ),
        )

        return drawingResultResponse
    }

    fun makeDrawingBoard(
        surveyId: UUID,
        boardSize: Int,
        surveyRewards: List<SurveyReward>,
    ) {
        // TODO: 적절한 다른 패키지간 도메인 변환 로직 도입
        val rewards =
            surveyRewards
                .map {
                    Reward(
                        name = it.name,
                        category = it.category,
                        count = it.count,
                    )
                }.toTypedArray()

        val drawingBoard =
            DrawingBoard.create(
                surveyId = surveyId,
                boardSize = boardSize,
                rewards = rewards,
            )
        drawingBoardAdapter.save(drawingBoard)
    }
}