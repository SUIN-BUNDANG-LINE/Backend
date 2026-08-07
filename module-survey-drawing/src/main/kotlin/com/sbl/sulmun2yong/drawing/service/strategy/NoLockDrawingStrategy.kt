package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * 무보호 기준선 (비교 실험 전용) — 경쟁 제어 지점이 **없다**.
 *
 * 트랜잭션 하나만 열고 곧바로 보드를 읽고 쓴다. 동시 요청은 InnoDB 행 락에 맡겨지며,
 * 그 대가(데드락 롤백)를 그대로 드러내는 것이 이 경로의 목적이다.
 *
 * `@Transactional` 대신 `TransactionTemplate` 을 쓰는 이유는 커밋을 `attempt {}` **안**에 넣기
 * 위함이다. 애노테이션은 트랜잭션 경계가 이 메서드 바깥이라 커밋 시점 실패(데드락·행 소실)가
 * 계측 밖에서 터진다 — 다섯 전략의 시도 지표가 같은 경계에서 기록되어야 대조가 성립한다.
 */
@Component
class NoLockDrawingStrategy(
    transactionManager: PlatformTransactionManager,
    drawingBoardRepository: DrawingBoardRepository,
    drawingHistoryRepository: DrawingHistoryRepository,
    surveyRepository: SurveyRepository,
    encryptionUtils: EncryptionUtils,
    drawingProcessMetrics: DrawingProcessMetrics,
    drawingEventPublisher: DrawingEventPublisher,
) : AbstractDrawingStrategy(
        drawingBoardRepository,
        drawingHistoryRepository,
        surveyRepository,
        encryptionUtils,
        drawingProcessMetrics,
        drawingEventPublisher,
    ) {
    override val mode = DrawMode.NO_LOCK

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        recordEntry() // 진입 제어 없음 — 대기 0 으로 기록해 시계열을 맞춘다
        return attempt {
            transactionTemplate.execute { draw(surveyId, participantId, selectedNumber, phoneNumber) }!!
        }
    }
}
