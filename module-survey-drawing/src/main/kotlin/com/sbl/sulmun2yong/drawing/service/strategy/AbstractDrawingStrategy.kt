package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.publisher.DrawingEventPublisher
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.hibernate.exception.LockAcquisitionException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.util.UUID

enum class DrawMode {
    NO_LOCK,
    SERIALIZABLE,
    OPTIMISTIC_RETRY,
    SYNCHRONIZED,
    REDISSON,
}

abstract class AbstractDrawingStrategy(
    protected val drawingBoardRepository: DrawingBoardRepository,
    protected val drawingHistoryRepository: DrawingHistoryRepository,
    protected val surveyRepository: SurveyRepository,
    protected val encryptionUtils: EncryptionUtils,
    protected val drawingProcessMetrics: DrawingProcessMetrics,
    protected val drawingEventPublisher: DrawingEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(AbstractDrawingStrategy::class.java)
    }

    /**
     * 디스패치 키 — DrawingBoardService 가 모드로 전략을 색인한다.
     *
     * 생성자 프로퍼티로 두면 안 된다: Kotlin 생성자 프로퍼티의 getter 는 final 이고(이 클래스엔
     * @Component 가 없어 allopen 도 적용되지 않는다), CGLIB 프록시는 final 을 오버라이드하지 못해
     * 초기화되지 않은 프록시 필드를 읽어 null 이 된다. abstract 로 두면 자식의 override getter 가
     * open 이라 프록시가 타겟에 위임한다.
     */
    abstract val mode: DrawMode

    abstract fun processDrawing(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse

    /**
     * 임계구역 진입을 통일 지표로 기록한다.
     *
     * 진입 제어가 없는 전략(NO_LOCK·SERIALIZABLE·OPTIMISTIC_RETRY)도 `대기 0 · success` 로 기록해
     * 다섯 전략이 **항상 같은 시계열**을 갖게 한다 — "값이 없는 것"과 "0"을 구분해야 대조가 성립한다.
     */
    protected fun recordEntry(
        waitNanos: Long = 0L,
        acquired: Boolean = true,
    ) {
        drawingProcessMetrics.recordContentionWait(mode.name, waitNanos)
        drawingProcessMetrics.recordEntry(mode.name, if (acquired) "success" else "failure")
    }

    /**
     * 시도 1회를 감싸 결과를 통일 지표(drawing_attempt_total{mode,result})로 기록한다.
     *
     * 성공이든 실패든 **모든 종료 경로**를 세므로, 재시도가 없는 전략은 시도 합계가 요청 수와
     * 같아지고 재시도가 있는 전략만 그보다 커진다 — 그 배수가 곧 재시도 낭비다.
     *
     * 호출부는 트랜잭션 커밋까지 이 블록 **안**에서 끝내야 한다. 커밋이 바깥이면 커밋 시점 실패가
     * 기록되지 않아 그 전략만 시도가 전부 성공으로 보인다.
     */
    protected fun <T> attempt(block: () -> T): T =
        try {
            block().also { drawingProcessMetrics.recordAttempt(mode.name, "success") }
        } catch (e: BoardVersionConflictException) {
            drawingProcessMetrics.recordAttempt(mode.name, "version_conflict")
            throw e
        } catch (e: ObjectOptimisticLockingFailureException) {
            // 경합 중 사라진 티켓 행 — Hibernate 가 UPDATE/DELETE 0행을 보고 올린다 (버전과 무관)
            drawingProcessMetrics.recordAttempt(mode.name, "stale_row")
            throw e
        } catch (e: CannotAcquireLockException) {
            drawingProcessMetrics.recordAttempt(mode.name, "deadlock")
            throw e
        } catch (e: LockAcquisitionException) {
            // 지연 로딩 조회에서 난 데드락 — 리포지토리를 거치지 않아 Spring 예외 변환을 타지 않는다.
            // SERIALIZABLE 은 평범한 SELECT 도 공유 락 읽기가 되어 이 경로로 데드락이 난다.
            drawingProcessMetrics.recordAttempt(mode.name, "deadlock")
            throw e
        } catch (e: Exception) {
            // 정상 거절(중복 참여·선택된 티켓)과 그 밖의 실패 — 세지 않으면 시도 합계가 요청 수에 못 미친다
            drawingProcessMetrics.recordAttempt(mode.name, "other")
            throw e
        }

    /**
     * 보드 조회 — 다섯 전략 모두 락 없는 일반 조회를 쓴다. 경쟁 제어는 각 전략이 이 바깥에서 건다.
     * 판정에 티켓 전량이 필요하므로 한 문장으로 함께 읽는다.
     */
    private fun findBoard(surveyId: UUID): DrawingBoard =
        drawingBoardRepository
            .findBySurveyIdWithTickets(surveyId)
            .orElseThrow { InvalidDrawingBoardException() }

    /** 추첨 본체 — 호출 시점에 트랜잭션이 열려 있어야 한다. */
    protected fun draw(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        // 추첨 가능 여부 조회
        val drawingBoard = findBoard(surveyId)
        val drawingResult = drawingBoard.getDrawingResult(selectedNumber)

        // 이미 추첨 참여했는지 검증
        val phoneNumberData = PhoneNumber.createWithNonNullable(phoneNumber)
        val existingHistory =
            drawingHistoryRepository
                .findBySurveyIdAndParticipantIdOrPhoneNumber(
                    surveyId,
                    participantId,
                    encryptionUtils.encrypt(phoneNumberData.value),
                ).orElse(null)
        if (existingHistory != null) {
            throw AlreadyParticipatedDrawingException()
        }

        // 추첨 결과 저장
        val changedDrawingBoard = drawingResult.changedDrawingBoard
        drawingBoardRepository.save(changedDrawingBoard)
        drawingHistoryRepository.save(
            DrawingHistory.create(
                participantId = participantId,
                phoneNumber = phoneNumberData,
                surveyId = surveyId,
                selectedTicketIndex = selectedNumber,
                ticket = changedDrawingBoard.tickets[selectedNumber],
            ),
        )
        drawingProcessMetrics.recordPersisted(isWinner = drawingResult is DrawingResult.Winner)

        closeSurveyIfTicketsExhausted(surveyId, changedDrawingBoard)

        drawingEventPublisher.publishCompleted(
            surveyId = surveyId,
            participantId = participantId,
            selectedNumber = selectedNumber,
            changedDrawingBoard = changedDrawingBoard,
        )

        return when (drawingResult) {
            is DrawingResult.Winner -> DrawingResultResponse.Winner(drawingResult.rewardName)
            is DrawingResult.NonWinner -> DrawingResultResponse.NonWinner()
        }
    }

    // 잔여 티켓이 0이 된 시점에 추첨 트랜잭션 안에서 설문을 즉시 종료한다.
    // 동일 트랜잭션이라 추첨 결과와 설문 종료가 원자적으로 커밋된다.
    private fun closeSurveyIfTicketsExhausted(
        surveyId: UUID,
        changedDrawingBoard: DrawingBoard,
    ) {
        if (changedDrawingBoard.remainingTicketCount > 0) return

        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalseWithLock(surveyId)
                .orElseThrow { SurveyNotFoundException() }

        if (survey.status == SurveyStatus.CLOSED) {
            log.info("이미 종료된 설문, mode: {}, surveyId: {}", mode, survey.id)
            return
        }

        surveyRepository.save(survey.finish())
        log.info("티켓 소진으로 종료, mode: {}, surveyId: {}", mode, survey.id)
    }
}
