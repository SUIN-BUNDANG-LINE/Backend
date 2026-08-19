package com.sbl.sulmun2yong.drawing.service.strategy

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.drawing.repository.TicketRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import java.util.*

/**
 * UNIQUE 위반의 정체 판별 — 예외 사슬의 메시지에 위반한 제약명이 실려 온다.
 * uk_drawing_histories_survey_phone 이면 중복 참여(정상 거절), 아니면 티켓 중복 배정(사고)이다.
 */
internal fun isPhoneDuplicate(e: DataIntegrityViolationException): Boolean =
    generateSequence<Throwable>(e) { it.cause }
        .any { it.message?.contains("uk_drawing_histories_survey_phone") == true }

/**
 * CannotAcquireLockException 의 정체 판별 — MySQL 은 데드락(1213)과 락 대기 타임아웃(1205)을
 * 같은 Spring 타입으로 수렴시키므로, 원인 사슬의 벤더 에러 코드로 가른다.
 * 1205 는 "사이클 없는 긴 대기"의 포기라 의미상 Redisson 의 대기 초과와 같은 lock_timeout 이다.
 */
internal fun isLockWaitTimeout(e: CannotAcquireLockException): Boolean =
    generateSequence<Throwable>(e) { it.cause }
        .any { it is SQLException && it.errorCode == 1205 }

enum class DrawMode {
    DEFAULT, // 기본 판정 (조건부 UPDATE — is_selected 가 버전 술어)
    SERIALIZABLE,
    SYNCHRONIZED,
    REDISSON,
}

abstract class AbstractDrawingStrategy(
    protected val drawingBoardRepository: DrawingBoardRepository,
    protected val ticketRepository: TicketRepository,
    protected val drawingHistoryRepository: DrawingHistoryRepository,
    protected val surveyRepository: SurveyRepository,
    protected val drawingProcessMetrics: DrawingProcessMetrics,
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
     * 임계구역 진입 대기를 통일 지표(drawing_contention_wait_seconds{mode})로 기록한다.
     *
     * 진입 제어가 없는 전략(DEFAULT·SERIALIZABLE)도 **0 을 기록**해
     * 다섯 전략이 **항상 같은 시계열**을 갖게 한다 — "값이 없는 것"과 "0"을 구분해야 대조가 성립한다.
     */
    protected fun recordEntry(waitNanos: Long = 0L) {
        drawingProcessMetrics.recordContentionWait(mode.name, waitNanos)
    }

    /**
     * 보드 조회 — 다섯 전략 모두 락 없는 일반 조회를 쓴다. 경쟁 제어는 각 전략이 이 바깥에서 건다.
     * 판정에 티켓 전량이 필요하므로 한 문장으로 함께 읽는다.
     */
    private fun findBoard(surveyId: UUID): DrawingBoard =
        drawingBoardRepository
            .findBySurveyId(surveyId)
            .orElseThrow {
                InvalidDrawingBoardException()
            }

    /**
     * 칸을 차지한다 — 경쟁 제어의 **가장 안쪽 판정**이다.
     *
     * 기본 판정: `is_selected = 0` 일 때만 뒤집고, 갱신 행이 0이면 이미 남이 가져간 것이다.
     * 읽고 나서 판단하지 않고 쓰면서 판단하므로 그 사이에 끼어들 틈이 없다.
     */
    protected open fun claimTicket(
        boardId: UUID,
        index: Int,
    ): Boolean = ticketRepository.markSelectedCAS(boardId, index) > 0

    /** 추첨 본체 — 호출 시점에 트랜잭션이 열려 있어야 한다. */
    protected fun draw(
        surveyId: UUID,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val board = findBoard(surveyId)

        val phoneNumberData = PhoneNumber.createWithNonNullable(phoneNumber)

        if (!claimTicket(board.id, selectedNumber)) throw AlreadySelectedTicketException()

        val ticketEntity =
            ticketRepository.findByBoardAndIndex(board.id, selectedNumber)
                ?: throw InvalidDrawingBoardException()

        val ticket = ticketEntity.toDomain()
        val remaining = ticketRepository.countRemaining(board.id).toInt()

        drawingHistoryRepository.save(
            DrawingHistory.create(participantId, phoneNumberData, surveyId, selectedNumber, ticket),
        )
        closeSurveyIfTicketsExhausted(surveyId, remaining)

        return when (ticket) {
            is Ticket.Winning -> DrawingResultResponse.Winner(ticket.rewardName)
            is Ticket.NonWinning -> DrawingResultResponse.NonWinner()
        }
    }

    // 잔여 티켓이 0이 된 시점에 추첨 트랜잭션 안에서 설문을 즉시 종료한다.
    // 동일 트랜잭션이라 추첨 결과와 설문 종료가 원자적으로 커밋된다.
    private fun closeSurveyIfTicketsExhausted(
        surveyId: UUID,
        remaining: Int,
    ) {
        if (remaining > 0) return

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
