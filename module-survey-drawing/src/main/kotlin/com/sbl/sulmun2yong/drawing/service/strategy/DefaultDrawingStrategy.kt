package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.drawing.repository.TicketRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * 기본 판정 (조건부 UPDATE) — 경쟁 제어 지점은 **그 칸의 상태값**이다.
 *
 * `UPDATE tickets SET is_selected = 1 WHERE ... AND is_selected = 0` 한 문장이 판정을 겸한다.
 * `is_selected = 0` 이 곧 버전 술어다 — "내가 본 상태 그대로일 때만 바꾼다". 상태 전이가
 * false → true 단방향이라 값이 왕복하지 않으므로 별도 version 컬럼이 필요 없다.
 *
 * 판정 단위가 **실제 경합 단위(칸 하나)와 같다**. 서로 다른 칸을 고른 요청은 서로 다른 행을
 * 건드리므로 아예 만나지 않는다 — 보드 단위로 판정하는 전략과 대조되는 지점이다.
 *
 * 락을 걸지 않으므로 임계구역 진입 대기가 없고, 경합은 DB 행 락이 흡수한다.
 */
@Component
class DefaultDrawingStrategy(
    transactionManager: PlatformTransactionManager,
    drawingBoardRepository: DrawingBoardRepository,
    ticketRepository: TicketRepository,
    drawingHistoryRepository: DrawingHistoryRepository,
    surveyRepository: SurveyRepository,
    drawingProcessMetrics: DrawingProcessMetrics,
) : AbstractDrawingStrategy(
        drawingBoardRepository = drawingBoardRepository,
        ticketRepository = ticketRepository,
        drawingHistoryRepository = drawingHistoryRepository,
        surveyRepository = surveyRepository,
        drawingProcessMetrics = drawingProcessMetrics,
    ) {
    override val mode = DrawMode.DEFAULT

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        recordEntry() // 진입 제어 없음 — 대기 0 으로 기록해 시계열을 맞춘다
        return transactionTemplate.execute { draw(surveyId, participantId, selectedNumber, phoneNumber) }!!
    }
}
