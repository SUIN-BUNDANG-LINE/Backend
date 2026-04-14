package com.sbl.sulmun2yong.consumer

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.drawing.service.DrawingProcessService
import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.util.DateUtil
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
 * F4: Consumer 멱등성 검증 — 추첨 티켓 전량 소진 시 중복 처리 방지
 *
 * 시나리오:
 * 1. 설문(boardSize=3, winning=1) + DrawingBoard 생성
 * 2. 3명이 순차적으로 추첨 → 티켓 전량 소진 → drawing-completed 이벤트 다수 발행
 * 3. Consumer 멱등성 검증:
 *    - drawing-auto-close: 이미 CLOSED인 설문에 대해 finish() 중복 호출 안 함
 *    - drawing-notification: unique constraint(event_id + notification_type)으로 중복 SMS job 생성 방지
 *
 * 실행 전제:
 * - 3-broker Kafka 클러스터 온라인
 * - MySQL, Redis 온라인
 */
@SpringBootTest
@Tag("concurrency")
class ConsumerIdempotencyIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(ConsumerIdempotencyIntegrationTest::class.java)
        private const val BOARD_SIZE = 3
        private const val WINNING_COUNT = 1
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
    private lateinit var transactionTemplate: TransactionTemplate

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

            // sms_notification_jobs 정리
            smsNotificationJobRepository.findAll()
                .filter { it.notificationType == "DRAWING_SMS" && it.payload.contains(testSurveyId.toString()) }
                .forEach { smsNotificationJobRepository.deleteById(it.id) }
        }
    }

    @Test
    fun `F4 - 티켓 전량 소진 후 drawing-completed 이벤트 다수 발행되어도 Consumer가 중복 처리하지 않는다`() {
        // given: 참여자 3명 생성
        val phoneNumbers = listOf("010-8000-0001", "010-8000-0002", "010-8000-0003")
        val participantInfos =
            phoneNumbers.mapIndexed { index, phone ->
                val participant =
                    Participant.create(
                        visitorId = UUID.randomUUID().toString(),
                        surveyId = testSurveyId,
                        userId = null,
                    )
                participantRepository.save(participant)
                testParticipantIds.add(participant.id)
                Triple(participant.id, phone, index)
            }

        // when: 3명이 순차적으로 추첨 (티켓 전량 소진)
        participantInfos.forEach { (participantId, phoneNumber, selectedNumber) ->
            drawingProcessService.processDrawing(
                surveyId = testSurveyId,
                participantId = participantId,
                selectedNumber = selectedNumber,
                phoneNumber = phoneNumber,
            )
            log.info("추첨 완료: participantId={}, selectedNumber={}", participantId, selectedNumber)
        }
        log.info("3명 순차 추첨 완료 — 티켓 전량 소진")

        // then 1: drawing-auto-close Consumer → 설문 CLOSED
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted {
                val currentStatus =
                    surveyRepository.findById(testSurveyId).orElseThrow().status
                assertEquals(SurveyStatus.CLOSED, currentStatus, "티켓 소진 후 설문이 자동 종료되어야 함")
            }
        log.info("검증 통과: survey 자동 종료 완료 (drawing-auto-close Consumer)")

        // then 2: SMS job 수 기록 (당첨자 수만큼 생성되어야 함)
        val smsJobCountAfterClose =
            smsNotificationJobRepository.findAll()
                .filter { it.notificationType == "DRAWING_SMS" && it.payload.contains(testSurveyId.toString()) }
                .size
        log.info("CLOSED 시점 SMS job 수: {}", smsJobCountAfterClose)

        // then 3: Outbox 전량 PUBLISHED 대기
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

        // then 4: 10초 추가 대기 후 SMS job 수가 변하지 않았는지 확인 (멱등성 검증)
        log.info("멱등성 검증을 위해 10초 추가 대기 시작...")
        Thread.sleep(10_000)

        val smsJobCountAfterWait =
            smsNotificationJobRepository.findAll()
                .filter { it.notificationType == "DRAWING_SMS" && it.payload.contains(testSurveyId.toString()) }
                .size
        assertEquals(
            smsJobCountAfterClose,
            smsJobCountAfterWait,
            "추가 대기 후에도 SMS job 수가 변하지 않아야 함 (중복 생성 방지)",
        )
        log.info("검증 통과: SMS job 수 불변 — {} → {} (멱등성 보장)", smsJobCountAfterClose, smsJobCountAfterWait)

        // then 5: Survey 여전히 CLOSED 확인
        val finalStatus =
            surveyRepository.findById(testSurveyId).orElseThrow().status
        assertEquals(SurveyStatus.CLOSED, finalStatus, "설문이 여전히 CLOSED 상태여야 함 (중복 finish() 호출 방지)")
        log.info("검증 통과: survey 여전히 CLOSED 상태")
    }

    /** 설문 생성 (즉시 추첨, boardSize=3, 당첨=1) */
    private fun createSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "테스트", category = "테스트", count = WINNING_COUNT)),
                    targetParticipantCount = BOARD_SIZE,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            it
                .updateContent(
                    title = "멱등성 테스트",
                    description = "",
                    thumbnail = null,
                    finishMessage = "",
                    rewardSetting = rewardSetting,
                    isVisible = true,
                    isResultOpen = false,
                    sections = listOf(Section.create()),
                ).start()
        }
}
