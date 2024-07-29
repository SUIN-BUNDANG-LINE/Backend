package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.domain.Reward
import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.dto.response.NonWinnerDrawingResultResponse
import com.sbl.sulmun2yong.drawing.dto.response.WinnerDrawingResultResponse
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import com.sbl.sulmun2yong.survey.domain.Reward as SurveyReward

@Service
@Transactional
class DrawingBoardService(
    private val drawingBoardAdapter: DrawingBoardAdapter,
    private val participantAdapter: ParticipantAdapter,
) {
    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val drawingBoard = drawingBoardAdapter.getBySurveyId(surveyId)
        return DrawingBoardResponse.of(drawingBoard)
    }

    fun doDrawing(
        participantId: UUID,
        selectedNumber: Int,
    ): DrawingResultResponse {
        val participant = participantAdapter.getParticipantById(participantId)
        val drawingBoard = drawingBoardAdapter.getBySurveyId(participant.surveyId)
        val drawingMachine = DrawingMachine(drawingBoard, selectedNumber)
        drawingMachine.insertQuarter()
        drawingMachine.selectTicket()
        val drawingResultResponse =
            if (drawingMachine.openTicketAndCheckIsWon()) {
                WinnerDrawingResultResponse.create(drawingMachine.getRewardName())
            } else {
                NonWinnerDrawingResultResponse.create()
            }
        drawingBoardAdapter.save(drawingBoard)
        return drawingResultResponse
    }

    fun makeDrawingBoard(
        surveyId: UUID,
        boardSize: Int,
        surveyRewards: List<SurveyReward>,
    ) {
        val rewards =
            surveyRewards
                .map {
                    Reward(
                        id = it.id,
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
