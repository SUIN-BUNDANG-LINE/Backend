package com.sbl.sulmun2yong.survey

import com.sbl.sulmun2yong.IntegrationTest
import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.FinishedAt
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.ResponseRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import com.sbl.sulmun2yong.survey.service.SurveyResponseService
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
 * Phase 1-9 검증 B: 응답 제출 동시성 + survey-response-submitted Fan-out
 *
 * 시나리오:
 * 1. 설문 (ImmediateDrawSetting, targetParticipantCount=50, boardSize=200) 생성
 * 2. 100명이 동시에 응답 제출
 * 3. Kafka Fan-out 검증:
 *    - auto-close Consumer → targetParticipantCount 도달 시 설문 CLOSED
 *    - Outbox 전량 PUBLISHED
 *
 * 실행 전제:
 * - 3-broker Kafka 클러스터 온라인
 */
@IntegrationTest
@Tag("concurrency")
class SurveyResponseKafkaIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(SurveyResponseKafkaIntegrationTest::class.java)
        private const val TARGET_PARTICIPANT_COUNT = 50
        private const val BOARD_SIZE = 200
        private const val SUBMISSION_COUNT = 100
    }

    @Autowired
    private lateinit var surveyResponseService: SurveyResponseService

    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var participantRepository: ParticipantRepository

    @Autowired
    private lateinit var responseRepository: ResponseRepository

    @Autowired
    private lateinit var outboxEventRepository: OutboxEventRepository

    @Autowired
    private lateinit var transactionTemplate: org.springframework.transaction.support.TransactionTemplate

    private lateinit var testSurveyId: UUID
    private lateinit var testSectionId: UUID

    @BeforeEach
    fun setup() {
        val survey = createSurvey()
        surveyRepository.save(survey)
        testSurveyId = survey.id
        testSectionId = survey.sectionEntities.first().id

        log.info(
            "테스트 셋업 완료: surveyId={}, targetParticipantCount={}",
            testSurveyId,
            TARGET_PARTICIPANT_COUNT,
        )
    }

    @AfterEach
    fun cleanup() {
        transactionTemplate.execute {
            responseRepository.findBySurveyId(testSurveyId).forEach { responseRepository.deleteById(it.id) }
            participantRepository.findBySurveyId(testSurveyId).forEach { participantRepository.deleteById(it.id) }
            surveyRepository.deleteById(testSurveyId)

            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
                .forEach { outboxEventRepository.deleteById(it.id) }
        }
    }

    @Test
    fun `100명 동시 응답 제출 - targetParticipantCount 도달 시 설문 자동 종료`() {
        // given
        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(SUBMISSION_COUNT)
        val results = mutableListOf<String>()

        // when: 100명이 동시에 응답 제출
        (1..SUBMISSION_COUNT).forEach { idx ->
            executor.submit {
                try {
                    startLatch.await()
                    try {
                        val request = createResponseRequest(idx)
                        surveyResponseService.responseToSurvey(testSurveyId, request)
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
        log.info("응답 제출 완료: 성공={}, 전체={}", successCount, SUBMISSION_COUNT)

        // then 1: 모든 응답이 성공해야 함 (auto-close는 비동기이므로 제출 시점에는 설문이 아직 IN_PROGRESS)
        // 단, 일부가 SurveyClosedException으로 실패할 수 있음 (Consumer가 매우 빨리 처리한 경우)
        assertTrue(successCount >= TARGET_PARTICIPANT_COUNT, "최소 targetParticipantCount만큼 응답이 성공해야 함")

        // then 2: Outbox 이벤트가 성공한 응답 수만큼 생성
        val outboxEvents =
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
        assertEquals(successCount, outboxEvents.size, "Outbox 이벤트 수가 응답 성공 수와 일치해야 함")

        // then 3: auto-close Consumer → 설문 CLOSED (targetParticipantCount 도달)
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted {
                val currentStatus =
                    surveyRepository.findById(testSurveyId).orElseThrow().status
                assertEquals(
                    SurveyStatus.CLOSED,
                    currentStatus,
                    "targetParticipantCount 도달 후 설문이 자동 종료되어야 함",
                )
            }
        log.info("검증 통과: survey 자동 종료 완료 (auto-close Consumer, targetParticipantCount={})", TARGET_PARTICIPANT_COUNT)

        // then 4: Outbox 전량 PUBLISHED
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted {
                val publishedCount =
                    outboxEventRepository.findAll()
                        .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
                        .count { it.status == OutboxStatus.PUBLISHED }
                assertEquals(successCount, publishedCount, "Outbox 이벤트 전량이 PUBLISHED 상태여야 함")
            }
        log.info("검증 통과: Outbox 전량 PUBLISHED")

        // then 5: 참여자 수 검증
        val participantCount = participantRepository.findBySurveyId(testSurveyId).size
        assertEquals(successCount, participantCount, "참여자 수가 응답 성공 수와 일치해야 함")
        log.info("검증 통과: 참여자 수={}", participantCount)
    }

    private fun createSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "아메리카노", category = "커피", count = 10)),
                    targetParticipantCount = TARGET_PARTICIPANT_COUNT,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            val section = Section.create()
            it
                .updateContent(
                    title = "통합 테스트 설문",
                    description = "Phase 1-9 응답 제출 통합 테스트",
                    thumbnail = null,
                    finishMessage = "감사합니다",
                    rewardSetting = rewardSetting,
                    isVisible = true,
                    isResultOpen = false,
                    sections = listOf(section),
                ).start()
        }

    private fun createResponseRequest(idx: Int): SurveyResponseRequest =
        SurveyResponseRequest(
            sectionResponses =
                listOf(
                    SurveyResponseRequest.SectionResponseRequest(
                        sectionId = testSectionId,
                        questionResponses = emptyList(),
                    ),
                ),
            visitorId = "visitor-$testSurveyId-$idx",
        )
}
