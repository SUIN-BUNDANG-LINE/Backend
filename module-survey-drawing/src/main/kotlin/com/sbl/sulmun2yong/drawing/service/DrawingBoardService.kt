package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.FinishedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardAccessException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.service.strategy.AbstractDrawingStrategy
import com.sbl.sulmun2yong.drawing.service.strategy.DrawMode
import com.sbl.sulmun2yong.drawing.service.strategy.isLockWaitTimeout
import com.sbl.sulmun2yong.drawing.service.strategy.isPhoneDuplicate
import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.InvalidParticipantException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.util.*

@Service
class DrawingBoardService(
    private val surveyRepository: SurveyRepository,
    private val participantRepository: ParticipantRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingProcessMetrics: DrawingProcessMetrics,
    strategies: List<AbstractDrawingStrategy>,
) {
    // 4개 전략(DEFAULT·SERIALIZABLE·SYNCHRONIZED·REDISSON)을 모드로 색인
    private val strategyByMode: Map<DrawMode, AbstractDrawingStrategy> =
        strategies.associateBy { it.mode }

    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val surveyStatus =
            surveyRepository
                .findByIdAndIsDeletedFalse(surveyId)
                .orElseThrow { SurveyNotFoundException() }
                .status
        if (surveyStatus == SurveyStatus.NOT_STARTED || surveyStatus == SurveyStatus.CLOSED) {
            throw InvalidDrawingBoardAccessException()
        }
        val drawingBoard =
            drawingBoardRepository
                .findBySurveyIdWithTickets(surveyId)
                .orElseThrow { InvalidDrawingBoardException() }
        return DrawingBoardResponse.of(drawingBoard)
    }

    /** 공통 검증(참가자 해석·설문 종료 여부) 후 지정된 전략으로 추첨을 위임한다. 운영 기본은 REDISSON. */
    fun doDrawing(
        mode: DrawMode,
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        val participant =
            participantRepository
                .findById(participantId)
                .orElseThrow { InvalidParticipantException() }
        val surveyId = participant.surveyId

        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalse(surveyId)
                .orElseThrow { SurveyNotFoundException() }
        if (survey.status == SurveyStatus.CLOSED) {
            throw FinishedDrawingException()
        }

        // 통제 실험을 위한 통일 계측 지점 — 다섯 전략이 모두 이곳을 지나므로
        // 결과(outcome)와 소요 시간(duration)이 같은 방식·같은 메트릭으로 기록된다.
        val startedAt = System.nanoTime()
        try {
            val result =
                strategyByMode
                    .getValue(mode)
                    .processDrawing(surveyId, participantId, selectedNumber, phoneNumber)
            drawingProcessMetrics.recordOutcome(mode.name, "success")
            // 적재 카운트는 커밋이 끝난 여기서 찍는다 — 전략(draw) 안에서 찍으면 롤백된 저장도
            // 세어져 성공 수와의 교차 검산(persisted = success)이 깨진다.
            drawingProcessMetrics.recordPersisted(isWinner = result is DrawingResultResponse.Winner)
            return result
        } catch (e: Exception) {
            drawingProcessMetrics.recordOutcome(mode.name, classifyOutcome(e))
            throw e
        } finally {
            drawingProcessMetrics.recordDuration(mode.name, System.nanoTime() - startedAt)
        }
    }

    private fun classifyOutcome(e: Exception): String =
        when (e) {
            // UNIQUE 위반 — 제약명으로 정체를 가른다: 티켓 중복 배정(사고) vs 중복 참여(정상 거절)
            is DataIntegrityViolationException -> if (isPhoneDuplicate(e)) "rejected" else "duplicate_ticket"

            is CannotAcquireLockException -> if (isLockWaitTimeout(e)) "lock_timeout" else "deadlock"

            is TooManyLockRequestException -> "lock_timeout"

            is AlreadySelectedTicketException -> "rejected"

            else -> "other"
        }
}
