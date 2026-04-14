package com.sbl.sulmun2yong.notification

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.drawing.service.DrawingProcessService
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import com.sbl.sulmun2yong.notification.service.LoggingSmsSender
import com.sbl.sulmun2yong.survey.domain.reward.FinishedAt
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.entity.Participant
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * SMS 실패 → Inbox 재시도 → DLT 전달 통합 테스트
 *
 * LoggingSmsSender.forcedFailCount를 제어하여 실패 동작을 시뮬레이션한다.
 * - forcedFailCount = 0: 항상 실패
 * - forcedFailCount = N (>0): N회까지 실패, 이후 성공
 * - forcedFailCount = -1: 기본 동작 (프로퍼티 기반)
 */
@SpringBootTest
@Tag("concurrency")
class SmsFailoverIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(SmsFailoverIntegrationTest::class.java)
        private const val BOARD_SIZE = 5
        private const val WINNING_COUNT = 1
    }

    @Autowired
    private lateinit var loggingSmsSender: LoggingSmsSender

    @Autowired
    private lateinit var drawingProcessService: DrawingProcessService

    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var participantRepository: ParticipantRepository

    @Autowired
    private lateinit var drawingBoardRepository: DrawingBoardRepository

    @Autowired
    private lateinit var drawingHistoryRepository: DrawingHistoryRepository

    @Autowired
    private lateinit var outboxEventRepository: OutboxEventRepository

    @Autowired
    private lateinit var smsNotificationJobRepository: SmsNotificationJobRepository

    @Autowired
    private lateinit var dltMessageRepository: DltMessageRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var testSurveyId: UUID
    private val testParticipantIds = mutableSetOf<UUID>()

    @BeforeEach
    fun setup() {
        val survey = createSurvey()
        surveyRepository.save(survey)
        testSurveyId = survey.id

        drawingBoardRepository.save(
            DrawingBoard.create(
                surveyId = survey.id,
                boardSize = BOARD_SIZE,
                rewards = survey.rewardSetting.rewards,
            ),
        )
        log.info("테스트 셋업 완료: surveyId={}", testSurveyId)
    }

    @AfterEach
    fun cleanup() {
        loggingSmsSender.resetTestState()
        transactionTemplate.execute {
            dltMessageRepository.findAll()
                .filter { it.payload.contains(testSurveyId.toString()) }
                .forEach { dltMessageRepository.deleteById(it.id) }
            smsNotificationJobRepository.findAll()
                .filter { it.notificationType == "DRAWING_SMS" && it.payload.contains(testSurveyId.toString()) }
                .forEach { smsNotificationJobRepository.deleteById(it.id) }
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Drawing" && it.aggregateId == testSurveyId.toString() }
                .forEach { outboxEventRepository.deleteById(it.id) }
            drawingBoardRepository.deleteBySurveyId(testSurveyId)
            drawingHistoryRepository.deleteBySurveyId(testSurveyId)
            testParticipantIds.forEach { participantRepository.deleteById(it) }
            surveyRepository.deleteById(testSurveyId)
        }
    }

    @Test
    fun `S4 - SmsSender 항상 실패시 재시도 후 FAILED 상태가 되고 DLT 메시지가 저장된다`() {
        // given: 항상 실패
        loggingSmsSender.forcedFailCount = 0

        // when: 전체 티켓 추첨
        drawAllTickets()

        // then 0: SMS job 생성 대기
        Awaitility.await()
            .atMost(120, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted {
                val jobs =
                    smsNotificationJobRepository.findAll()
                        .filter { it.notificationType == "DRAWING_SMS" && it.payload.contains(testSurveyId.toString()) }
                assertTrue(jobs.isNotEmpty(), "SMS job이 생성되어야 함")
                log.info("SMS job 생성 확인: count={}, status={}", jobs.size, jobs.map { it.status })
            }

        // then 1: FAILED 상태 전환 대기 (Worker 30초 폴링 × 재시도)
        Awaitility.await()
            .atMost(480, TimeUnit.SECONDS)
            .pollInterval(5, TimeUnit.SECONDS)
            .untilAsserted {
                val failedJobs =
                    smsNotificationJobRepository.findAll()
                        .filter {
                            it.notificationType == "DRAWING_SMS" &&
                                it.payload.contains(testSurveyId.toString()) &&
                                it.status == SmsJobStatus.FAILED
                        }
                assertTrue(failedJobs.isNotEmpty(), "당첨자의 SMS job이 FAILED 상태여야 함")
                assertEquals(6, failedJobs[0].retryCount, "MAX_RETRY(5) 초과 시 retryCount는 6이어야 함")
                log.info("검증 통과: SMS job FAILED, retryCount={}", failedJobs[0].retryCount)
            }

        // then 2: dlt_messages에 저장
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted {
                val dltMessages =
                    dltMessageRepository.findAll()
                        .filter { it.payload.contains(testSurveyId.toString()) }
                assertTrue(dltMessages.isNotEmpty(), "DLT 메시지가 저장되어야 함")
                assertTrue(dltMessages[0].eventId.isNotBlank(), "eventId가 기록되어야 함")
                assertEquals("DRAWING_SMS", dltMessages[0].notificationType)
                log.info("검증 통과: DLT 메시지 저장, eventId={}", dltMessages[0].eventId)
            }
    }

    @Test
    fun `F5 - SmsSender 3회 실패 후 4번째 성공시 COMPLETED 상태가 되고 DLT 메시지는 없다`() {
        // given: 3회까지 실패, 4번째부터 성공
        loggingSmsSender.forcedFailCount = 3

        // when: 전체 티켓 추첨
        drawAllTickets()

        // then 1: COMPLETED 상태
        Awaitility.await()
            .atMost(400, TimeUnit.SECONDS)
            .pollInterval(5, TimeUnit.SECONDS)
            .untilAsserted {
                val completedJobs =
                    smsNotificationJobRepository.findAll()
                        .filter {
                            it.notificationType == "DRAWING_SMS" &&
                                it.payload.contains(testSurveyId.toString()) &&
                                it.status == SmsJobStatus.COMPLETED
                        }
                assertTrue(completedJobs.isNotEmpty(), "SMS job이 COMPLETED 상태여야 함")
                log.info("검증 통과: SMS job COMPLETED, retryCount={}", completedJobs[0].retryCount)
            }

        // then 2: DLT 메시지 없음
        Thread.sleep(5000)
        val dltMessages =
            dltMessageRepository.findAll()
                .filter { it.payload.contains(testSurveyId.toString()) }
        assertEquals(0, dltMessages.size, "5회 미만 실패이므로 DLT 메시지가 없어야 함")
    }

    private fun createSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "아메리카노", category = "커피", count = WINNING_COUNT)),
                    targetParticipantCount = BOARD_SIZE,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            it.updateContent(
                title = "SMS 실패 테스트",
                description = "",
                thumbnail = null,
                finishMessage = "",
                rewardSetting = rewardSetting,
                isVisible = true,
                isResultOpen = false,
                sections = listOf(Section.create()),
            ).start()
        }

    private fun drawAllTickets() {
        for (i in 0 until BOARD_SIZE) {
            val participant = Participant.create(UUID.randomUUID().toString(), testSurveyId, null)
            participantRepository.save(participant)
            testParticipantIds.add(participant.id)
            try {
                drawingProcessService.processDrawing(
                    testSurveyId,
                    participant.id,
                    i,
                    "010-1234-${String.format("%04d", i + 1)}",
                )
            } catch (_: Exception) {
            }
        }
        log.info("전체 추첨 완료: boardSize={}", BOARD_SIZE)
    }
}
