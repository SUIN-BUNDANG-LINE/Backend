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
import org.slf4j.LoggerFactory
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

    /** 보드 조회 — 기본은 락 없는 일반 조회. 낙관락 전략만 FORCE_INCREMENT 로 바꾼다. */
    protected open fun findBoard(surveyId: UUID): DrawingBoard =
        drawingBoardRepository
            .findBySurveyId(surveyId)
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
