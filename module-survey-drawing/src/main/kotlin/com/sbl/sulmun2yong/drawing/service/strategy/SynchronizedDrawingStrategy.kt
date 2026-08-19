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
import java.util.*

/**
 * 메서드 수준 synchronized (비교 실험 전용) — 자바의 `synchronized void draw()` 에 대응하는
 * `@Synchronized` 를 그대로 쓴다. 한계를 실측하는 것이 이 경로의 목적이다:
 *
 *  ① **잠금 대상을 고를 수 없다** — 메서드 수준은 무조건 `this`(싱글턴 빈)를 잠근다.
 *    판정 단위가 "칸 하나"가 아니라 **서버 전체**가 되어, 다른 설문·다른 칸을 고른 요청까지
 *    한 줄로 선다 (논리적 충돌이 없는 부하에서 가짜 대기 발생 — TICKETS=0 으로 드러난다).
 *  ② **JVM 로컬** — 모니터가 인스턴스마다 별개라 web 을 2대로 늘리면 cross-JVM 경합이
 *    그대로 통과한다.
 *
 * 트랜잭션은 `TransactionTemplate` 으로 **락 안에서** 열고 닫는다. `@Transactional` 을 쓰면
 * 트랜잭션이 모니터 구간보다 바깥이 되어 락 해제 후 커밋 → 상호배제가 깨진다.
 */
@Component
class SynchronizedDrawingStrategy(
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
    override val mode = DrawMode.SYNCHRONIZED

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val waitStart = System.nanoTime()
        return drawSynchronized(waitStart, surveyId, participantId, selectedNumber, phoneNumber)
    }

    @Synchronized
    private fun drawSynchronized(
        waitStart: Long,
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        recordEntry(System.nanoTime() - waitStart)
        return transactionTemplate.execute {
            draw(surveyId, participantId, selectedNumber, phoneNumber)
        }!!
    }
}
