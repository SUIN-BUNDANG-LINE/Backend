package com.sbl.sulmun2yong.drawing

import com.sbl.sulmun2yong.IntegrationTest
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.drawing.service.DrawingProcessService
import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
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
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Phase 1-9 검증 A: 추첨 동시성 + Fan-out + Inbox 패턴
 *
 * 시나리오:
 * 1. 설문 + 추첨판(티켓 50장, 당첨 10장) + 참여자 100명 생성
 * 2. 100명이 동시에 추첨 시도 (ExecutorService + CountDownLatch)
 * 3. Kafka Fan-out 검증:
 *    - drawing-auto-close Consumer → 티켓 소진 시 설문 CLOSED
 *    - drawing-notification Consumer → 당첨자 sms_notification_jobs COMPLETED
 *
 * 실행 전제:
 * - 3-broker Kafka 클러스터 온라인
 * - 다른 Spring Boot 인스턴스가 Consumer Group에 조인하지 않은 상태
 */
@IntegrationTest
@Tag("concurrency")
class DrawingKafkaIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(DrawingKafkaIntegrationTest::class.java)
        private const val BOARD_SIZE = 50
        private const val WINNING_COUNT = 10
        private const val PARTICIPANT_COUNT = 100
    }

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
    private lateinit var transactionTemplate: org.springframework.transaction.support.TransactionTemplate

    private lateinit var testSurveyId: UUID
    private val testParticipantIds = mutableSetOf<UUID>()

    @BeforeEach
    fun setup() {
        val survey = createSurvey()
        surveyRepository.save(survey)
        testSurveyId = survey.id

        val drawingBoard =
            DrawingBoard.create(
                surveyId = survey.id,
                boardSize = BOARD_SIZE,
                rewards = survey.rewardSetting.rewards,
            )
        drawingBoardRepository.save(drawingBoard)

        log.info("테스트 셋업 완료: surveyId={}, boardSize={}, winningCount={}", testSurveyId, BOARD_SIZE, WINNING_COUNT)
    }

    @AfterEach
    fun cleanup() {
        transactionTemplate.execute {
            drawingBoardRepository.deleteBySurveyId(testSurveyId)
            drawingHistoryRepository.deleteBySurveyId(testSurveyId)
            testParticipantIds.forEach { participantRepository.deleteById(it) }
            surveyRepository.deleteById(testSurveyId)

            // Outbox 정리
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Drawing" && it.aggregateId == testSurveyId.toString() }
                .forEach { outboxEventRepository.deleteById(it.id) }

            // sms_notification_jobs 정리 (testSurveyId 관련 이벤트만)
            smsNotificationJobRepository.findAll()
                .filter { it.notificationType == "DRAWING_SMS" && it.payload.contains(testSurveyId.toString()) }
                .forEach { smsNotificationJobRepository.deleteById(it.id) }
        }
    }

    @Test
    fun `100명 동시 추첨 - Fan-out Consumer로 설문 자동 종료 + Inbox 패턴 SMS job 생성`() {
        // given: 참여자 100명 생성
        val participantInfos = createParticipants(PARTICIPANT_COUNT)

        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(PARTICIPANT_COUNT)
        val results = mutableListOf<String>()

        // when: 100명이 동시 추첨 시도
        participantInfos.forEachIndexed { index, (participantId, phoneNumber) ->
            executor.submit {
                try {
                    startLatch.await()
                    try {
                        drawingProcessService.processDrawing(
                            surveyId = testSurveyId,
                            participantId = participantId,
                            selectedNumber = index % BOARD_SIZE,
                            phoneNumber = phoneNumber,
                        )
                        synchronized(results) { results.add("success") }
                    } catch (e: Exception) {
                        synchronized(results) { results.add("failure: ${e.javaClass.simpleName}") }
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await(30, TimeUnit.SECONDS)
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        val successCount = results.count { it == "success" }
        log.info("추첨 완료: 성공={}, 전체={}", successCount, PARTICIPANT_COUNT)

        // then 1: 티켓 수만큼만 성공 (50건)
        assertEquals(BOARD_SIZE, successCount, "티켓 수만큼만 추첨이 성공해야 함")

        // then 2: 당첨자 수 정확 (10명)
        val winningHistoryCount =
            drawingHistoryRepository.findAll()
                .filter { it.surveyId == testSurveyId && it.ticketType == "WINNING" }
                .size
        assertEquals(WINNING_COUNT, winningHistoryCount, "당첨자 수가 설정값과 일치해야 함")

        // then 3: Outbox 이벤트가 추첨 성공 수만큼 생성됨
        val outboxEvents =
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Drawing" && it.aggregateId == testSurveyId.toString() }
        assertEquals(BOARD_SIZE, outboxEvents.size, "Outbox 이벤트 수가 추첨 성공 수와 일치해야 함")

        // then 4: drawing-auto-close Consumer → 설문 CLOSED
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted {
                val currentStatus =
                    surveyRepository.findById(testSurveyId).orElseThrow().status
                assertEquals(SurveyStatus.CLOSED, currentStatus, "티켓 소진 후 설문이 자동 종료되어야 함")
            }
        log.info("검증 통과: survey 자동 종료 완료 (drawing-auto-close Consumer)")

        // then 5: Outbox 전량 PUBLISHED
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted {
                val publishedCount =
                    outboxEventRepository.findAll()
                        .filter { it.aggregateType == "Drawing" && it.aggregateId == testSurveyId.toString() }
                        .count { it.status == OutboxStatus.PUBLISHED }
                assertEquals(BOARD_SIZE, publishedCount, "Outbox 이벤트 전량이 PUBLISHED 상태여야 함")
            }
        log.info("검증 통과: Outbox 전량 PUBLISHED")

        // then 6: drawing-notification Consumer → 당첨자만큼 sms_notification_jobs COMPLETED
        Awaitility.await()
            .atMost(90, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted {
                val completedJobCount =
                    smsNotificationJobRepository.findAll()
                        .filter {
                            it.notificationType == "DRAWING_SMS" &&
                                it.payload.contains(testSurveyId.toString()) &&
                                it.status == SmsJobStatus.COMPLETED
                        }.size
                assertEquals(
                    WINNING_COUNT,
                    completedJobCount,
                    "당첨자 수만큼 sms_notification_jobs가 COMPLETED여야 함 (Inbox + Listener)",
                )
            }
        log.info("검증 통과: sms_notification_jobs COMPLETED {} 건 (Inbox 패턴)", WINNING_COUNT)

        // then 7: 실패한 추첨은 모두 예상된 예외여야 함
        val failures = results.filter { it.startsWith("failure") }
        assertTrue(failures.all { it.contains("AlreadySelectedTicket") }, "실패는 모두 AlreadySelectedTicketException이어야 함")
    }

    private fun createSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "아메리카노", category = "커피", count = WINNING_COUNT)),
                    targetParticipantCount = BOARD_SIZE,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            val section = Section.create()
            it
                .updateContent(
                    title = it.title,
                    description = it.description,
                    thumbnail = it.thumbnail,
                    finishMessage = it.finishMessage,
                    rewardSetting = rewardSetting,
                    isVisible = it.isVisible,
                    isResultOpen = it.isResultOpen,
                    sections = listOf(section),
                ).start()
        }

    private fun createParticipants(count: Int): List<Pair<UUID, String>> =
        (1..count).map { idx ->
            val participant =
                Participant.create(
                    visitorId = UUID.randomUUID().toString(),
                    surveyId = testSurveyId,
                    userId = null,
                )
            participantRepository.save(participant)
            testParticipantIds.add(participant.id)
            Pair(participant.id, "010-9000-${String.format("%04d", idx)}")
        }
}
