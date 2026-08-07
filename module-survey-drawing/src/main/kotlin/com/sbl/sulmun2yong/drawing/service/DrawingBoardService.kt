package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.FinishedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardAccessException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.service.strategy.AbstractDrawingStrategy
import com.sbl.sulmun2yong.drawing.service.strategy.BoardVersionConflictException
import com.sbl.sulmun2yong.drawing.service.strategy.DrawMode
import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.InvalidParticipantException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.hibernate.exception.LockAcquisitionException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DrawingBoardService(
    private val surveyRepository: SurveyRepository,
    private val participantRepository: ParticipantRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingProcessMetrics: DrawingProcessMetrics,
    strategies: List<AbstractDrawingStrategy>,
) {
    // 5개 전략(NO_LOCK·SERIALIZABLE·OPTIMISTIC_RETRY·SYNCHRONIZED·REDISSON)을 모드로 색인
    private val strategyByMode: Map<DrawMode, AbstractDrawingStrategy> = strategies.associateBy { it.mode }

    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val surveyStatus =
            surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }.status
        if (surveyStatus == SurveyStatus.NOT_STARTED || surveyStatus == SurveyStatus.CLOSED) {
            throw InvalidDrawingBoardAccessException()
        }
        val drawingBoard =
            drawingBoardRepository.findBySurveyIdWithTickets(surveyId).orElseThrow { InvalidDrawingBoardException() }
        return DrawingBoardResponse.of(drawingBoard)
    }

    /** 공통 검증(참가자 해석·설문 종료 여부) 후 지정된 전략으로 추첨을 위임한다. 운영 기본은 REDISSON. */
    fun doDrawing(
        mode: DrawMode,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val participant = participantRepository.findById(participantId).orElseThrow { InvalidParticipantException() }
        val surveyId = participant.surveyId

        val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }
        if (survey.status == SurveyStatus.CLOSED) {
            throw FinishedDrawingException()
        }

        // 통제 실험을 위한 통일 계측 지점 — 다섯 전략이 모두 이곳을 지나므로
        // 결과(outcome)와 소요 시간(duration)이 같은 방식·같은 메트릭으로 기록된다.
        val startedAt = System.nanoTime()
        try {
            val result = strategyByMode.getValue(mode).processDrawing(surveyId, participantId, selectedNumber, phoneNumber)
            drawingProcessMetrics.recordOutcome(mode.name, "success")
            return result
        } catch (e: Exception) {
            drawingProcessMetrics.recordOutcome(mode.name, classifyOutcome(e))
            throw e
        } finally {
            drawingProcessMetrics.recordDuration(mode.name, System.nanoTime() - startedAt)
        }
    }

    // 실패 원인을 전략과 무관한 공통 어휘로 분류한다 — 그래야 5자 비교에서 실패 사유를 대조할 수 있다.
    private fun classifyOutcome(e: Exception): String =
        when (e) {
            is BoardVersionConflictException -> "version_conflict" // 낙관락 전략의 보드 버전 충돌
            is ObjectOptimisticLockingFailureException -> "stale_row" // 경합 중 사라진 티켓 행 (버전과 무관)
            // InnoDB 데드락 — 리포지토리를 거치면 Spring 예외로 변환되고, 지연 로딩 조회에서 나면
            // 변환을 타지 않아 Hibernate 고유 예외로 올라온다. 둘 다 같은 사건이므로 같이 센다.
            is CannotAcquireLockException, is LockAcquisitionException -> "deadlock"
            is TooManyLockRequestException -> "lock_timeout" // 분산락 대기 초과(429)
            is AlreadyParticipatedDrawingException, is AlreadySelectedTicketException -> "rejected" // 정상 거절
            else -> "other"
        }
}
