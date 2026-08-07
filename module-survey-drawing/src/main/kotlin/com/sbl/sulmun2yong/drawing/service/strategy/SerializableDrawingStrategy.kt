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
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * DB 안 직렬화 (비교 실험 전용) — 경쟁 제어 지점은 **트랜잭션 격리수준**이다.
 *
 * 이 요청의 트랜잭션만 SERIALIZABLE 로 열면 일반 SELECT 가 공유 락(S) 읽기가 되어,
 * 같은 보드를 읽은 두 트랜잭션이 배타 락(X) 승격을 다투다 데드락으로 한쪽이 롤백된다.
 * 그 비용(데드락·행 락 대기·롤백)을 계측하는 것이 이 경로의 목적이다.
 *
 * 격리수준은 `TransactionTemplate` 에 실어 요청 트랜잭션 단위로만 적용한다 — 커밋이 `attempt {}`
 * 안에서 일어나야 커밋 시점 실패가 다른 전략과 같은 경계에서 기록된다.
 */
@Component
class SerializableDrawingStrategy(
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
    override val mode = DrawMode.SERIALIZABLE

    private val transactionTemplate =
        TransactionTemplate(transactionManager).apply {
            isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE
        }

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
