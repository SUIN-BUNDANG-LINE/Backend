package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * 낙관락 전략이 스스로 검출한 버전 충돌.
 *
 * Hibernate 가 자체적으로 올리는 `ObjectOptimisticLockingFailureException`(경합으로 사라진 티켓 행)과
 * 구분하려고 전용 타입을 둔다 — 그래야 통일 지표에서 두 실패를 다른 이름으로 셀 수 있다.
 * 재시도 루프와 트랜잭션 롤백은 부모 타입으로 잡히므로 동작은 그대로다.
 */
class BoardVersionConflictException(
    surveyId: UUID,
) : ObjectOptimisticLockingFailureException(DrawingBoard::class.java, surveyId)

/**
 * 낙관적 락 + 재시도 (비교 실험 전용) — 경쟁 제어 지점은 **보드 버전 컬럼**이다.
 *
 * 버전 검사는 이 전략이 직접 수행한다. 엔티티에 `@Version` 을 달면 다른 네 전략의 추첨까지
 * 버전을 올려 그쪽에서도 충돌이 나므로, 무보호 기준선이 무보호가 아니게 되어 대조가 깨진다.
 * 그래서 `drawing_boards.version` 은 JPA 매핑에서 빼두고 이 전략만 네이티브 쿼리로 읽고 쓴다.
 *
 * 티켓 선택 상태가 자식 행(TicketEntity)에 있어 보드 행이 dirty 되지 않는다. 그래서 트랜잭션
 * 시작 시 버전을 읽어두고, 추첨을 마친 뒤 "읽은 값 그대로일 때만 +1" 조건부 UPDATE 로 커밋
 * 직전에 충돌을 판정한다 — 보드 전체를 하나의 낙관적 자원으로 만드는 장치다.
 *
 * 실패(버전 충돌·데드락)는 트랜잭션 단위로 재시도하며, 시도마다 원인별로 기록해 성공당 시도
 * 횟수(재시도 낭비)를 계측한다. 재시도 루프는 트랜잭션 **밖**이어야 하므로 `TransactionTemplate`
 * 으로 시도마다 새 트랜잭션을 연다.
 */
@Component
class OptimisticRetryDrawingStrategy(
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
    override val mode = DrawMode.OPTIMISTIC_RETRY

    companion object {
        private const val MAX_ATTEMPTS = 5
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        recordEntry() // 진입 제어 없음 — 충돌은 커밋 시점에 판정된다
        var lastFailure: RuntimeException? = null
        repeat(MAX_ATTEMPTS) {
            try {
                // 시도마다 독립 트랜잭션 — 충돌로 롤백되면 다음 시도가 새 트랜잭션을 연다.
                // attempt {} 가 시도 결과를 통일 지표로 기록하므로 성공당 시도 횟수가 드러난다.
                return attempt {
                    transactionTemplate.execute {
                        val readVersion = readBoardVersion(surveyId)
                        val result = draw(surveyId, participantId, selectedNumber, phoneNumber)
                        checkBoardUnchanged(surveyId, readVersion)
                        result
                    }!!
                }
            } catch (e: ObjectOptimisticLockingFailureException) {
                lastFailure = e
            } catch (e: CannotAcquireLockException) {
                // 버전 UPDATE 자체가 같은 행을 두드리는 쓰기라 데드락도 난다 — 이것도 재시도 대상
                lastFailure = e
            }
        }
        throw lastFailure ?: IllegalStateException("낙관락 재시도 소진")
    }

    private fun readBoardVersion(surveyId: UUID): Long =
        drawingBoardRepository.findVersionBySurveyId(surveyId) ?: throw InvalidDrawingBoardException()

    /**
     * 추첨 결과를 밀어 넣은 뒤(flushAutomatically) 조건부 UPDATE 로 버전을 올린다.
     * 갱신 행이 0이면 읽은 뒤 다른 추첨이 커밋된 것이므로 트랜잭션을 통째로 되돌린다.
     */
    private fun checkBoardUnchanged(
        surveyId: UUID,
        readVersion: Long,
    ) {
        if (drawingBoardRepository.increaseVersionIfUnchanged(surveyId, readVersion) == 0) {
            throw BoardVersionConflictException(surveyId)
        }
    }
}
