package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.drawing.dto.response.DrawingBoardResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.exception.FinishedDrawingException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardAccessException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.metrics.DrawingProcessMetrics
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.InvalidParticipantException
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DrawingBoardService(
    private val surveyRepository: SurveyRepository,
    private val participantRepository: ParticipantRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
    private val drawingProcessAdapter: DrawingProcessService,
    private val drawingProcessWithoutLockAdapter: DrawingProcessWithoutLockService,
    private val drawingProcessWithOptimisticLockAdapter: DrawingProcessWithOptimisticLockService,
    private val drawingProcessMetrics: DrawingProcessMetrics,
) {
    companion object {
        private const val OPTIMISTIC_MAX_ATTEMPTS = 5
    }

    // synchronized 실험용 — 설문별 JVM 로컬 모니터 객체. 인스턴스(JVM)마다 별개라는 점이 실험의 핵심.
    private val surveyLocks = ConcurrentHashMap<UUID, Any>()

    fun getDrawingBoard(surveyId: UUID): DrawingBoardResponse {
        val surveyStatus =
            surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElseThrow { SurveyNotFoundException() }.status
        if (surveyStatus == SurveyStatus.NOT_STARTED || surveyStatus == SurveyStatus.CLOSED) {
            throw InvalidDrawingBoardAccessException()
        }
        val drawingBoard = drawingBoardRepository.findBySurveyId(surveyId).orElseThrow { InvalidDrawingBoardException() }
        return DrawingBoardResponse.of(drawingBoard)
    }

    fun doDrawing(
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

        return drawingProcessAdapter.processDrawing(surveyId, participantId, selectedNumber, phoneNumber)
    }

    // 실험용 — 분산락 미적용. T8 race condition 비교 측정 전용.
    fun doDrawingWithoutLock(
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

        return drawingProcessWithoutLockAdapter.processDrawingWithoutLock(surveyId, participantId, selectedNumber, phoneNumber)
    }

    // 실험용 — 낙관적 락 (OPTIMISTIC_FORCE_INCREMENT). 3자 비교 측정 전용.
    fun doDrawingWithOptimisticLock(
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

        return drawingProcessWithOptimisticLockAdapter.processDrawingWithOptimisticLock(
            surveyId,
            participantId,
            selectedNumber,
            phoneNumber,
        )
    }

    // 실험용 — 낙관적 락 + 재시도. "실패하면 재시도하면 되지 않나"의 낭비(성공당 시도 횟수)를 계측한다.
    // 버전 충돌뿐 아니라 데드락(FORCE_INCREMENT 버전 갱신 경합)도 재시도하며, 시도마다 원인별로 기록한다.
    fun doDrawingWithOptimisticRetry(
        participantId: UUID,
        selectedNumber: Int,
        phoneNumber: String,
    ): DrawingResultResponse {
        var lastFailure: RuntimeException? = null
        repeat(OPTIMISTIC_MAX_ATTEMPTS) {
            try {
                val result = doDrawingWithOptimisticLock(participantId, selectedNumber, phoneNumber)
                drawingProcessMetrics.recordAttemptSuccess()
                return result
            } catch (e: ObjectOptimisticLockingFailureException) {
                drawingProcessMetrics.recordAttemptVersionConflict()
                lastFailure = e
            } catch (e: CannotAcquireLockException) {
                drawingProcessMetrics.recordAttemptDeadlock()
                lastFailure = e
            }
        }
        throw lastFailure ?: IllegalStateException("낙관락 재시도 소진")
    }

    // 실험용 — synchronized(JVM 로컬) 직렬화. 트랜잭션을 락 안에서 시작·커밋해 단일 인스턴스에선
    // 상호배제가 성립하지만, 모니터가 JVM마다 별개라 cross-JVM 경합은 막지 못한다 — 그 한계 실측 경로.
    fun doDrawingWithSynchronized(
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

        val lockObject = surveyLocks.computeIfAbsent(surveyId) { Any() }
        val waitStart = System.nanoTime()
        synchronized(lockObject) {
            drawingProcessMetrics.recordJvmLockWait(System.nanoTime() - waitStart)
            return drawingProcessWithoutLockAdapter.processDrawingWithoutLock(surveyId, participantId, selectedNumber, phoneNumber)
        }
    }
}
