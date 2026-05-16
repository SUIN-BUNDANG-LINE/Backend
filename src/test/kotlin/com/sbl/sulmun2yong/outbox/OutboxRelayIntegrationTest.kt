package com.sbl.sulmun2yong.outbox

import com.sbl.sulmun2yong.IntegrationTest
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventListener
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
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
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * S5 + F3 검증: Outbox Relay 보상 발행 통합 테스트
 *
 * 시나리오:
 * 1. OutboxEventListener(AFTER_COMMIT 정상 경로)를 @SpyBean으로 차단
 * 2. 설문 (ImmediateDrawSetting, targetParticipantCount=5) 생성 + start
 * 3. 5건 응답 제출 → Outbox 전량 PENDING 확인
 * 4. Spy 복구 → OutboxRelayScheduler(5초 폴링, 30초 age threshold)가 보상 발행
 * 5. auto-close Consumer가 설문을 CLOSED로 변경하는 전체 흐름 검증
 *
 * 실행 전제:
 * - 3-broker Kafka 클러스터 온라인
 */
@IntegrationTest
@Tag("concurrency")
class OutboxRelayIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(OutboxRelayIntegrationTest::class.java)
        private const val TARGET_PARTICIPANT_COUNT = 5
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
    private lateinit var transactionTemplate: TransactionTemplate

    @SpyBean
    private lateinit var outboxEventListener: OutboxEventListener

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
        reset(outboxEventListener)
    }

    @Test
    fun `정상 경로 차단 시 Outbox PENDING 유지 후 Relay가 보상 발행하여 설문 자동 종료`() {
        // given: 정상 경로(AFTER_COMMIT) 차단
        doNothing().whenever(outboxEventListener).handle(any<OutboxPublishEvent>())
        log.info("OutboxEventListener 정상 경로 차단 완료")

        // when: 5건 응답 제출
        (1..TARGET_PARTICIPANT_COUNT).forEach { idx ->
            val request = createResponseRequest(idx)
            surveyResponseService.responseToSurvey(testSurveyId, request)
            log.info("응답 제출 완료: visitorId=relay-test-{}", idx)
        }

        // then 1: Outbox 전량 PENDING 확인
        val outboxEvents =
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
        assertEquals(TARGET_PARTICIPANT_COUNT, outboxEvents.size, "Outbox 이벤트 수가 응답 수와 일치해야 함")
        assertTrue(
            outboxEvents.all { it.status == OutboxStatus.PENDING },
            "정상 경로 차단 시 Outbox 전량 PENDING 상태여야 함",
        )
        log.info("검증 통과: Outbox 전량 PENDING ({}건)", outboxEvents.size)

        // given: 정상 경로 복구 (OutboxRelay가 재발행 가능하도록)
        reset(outboxEventListener)
        log.info("OutboxEventListener 정상 경로 복구 완료 - Relay 보상 발행 대기 시작")

        // then 2: auto-close Consumer → 설문 CLOSED (최대 120초 대기: 30초 age + 5초 polling + 여유)
        Awaitility.await()
            .atMost(120, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted {
                val currentStatus = surveyRepository.findById(testSurveyId).orElseThrow().status
                assertEquals(
                    SurveyStatus.CLOSED,
                    currentStatus,
                    "Relay 보상 발행 후 auto-close Consumer가 설문을 CLOSED로 변경해야 함",
                )
            }
        log.info("검증 통과: Relay 보상 발행 → auto-close Consumer → 설문 자동 종료")

        // then 3: Outbox 전량 PUBLISHED 확인
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted {
                val publishedCount =
                    outboxEventRepository.findAll()
                        .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
                        .count { it.status == OutboxStatus.PUBLISHED }
                assertEquals(
                    TARGET_PARTICIPANT_COUNT,
                    publishedCount,
                    "Outbox 이벤트 전량이 PUBLISHED 상태여야 함",
                )
            }
        log.info("검증 통과: Outbox 전량 PUBLISHED")
    }

    private fun createSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "테스트", category = "테스트", count = 1)),
                    targetParticipantCount = TARGET_PARTICIPANT_COUNT,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            it.updateContent(
                title = "Relay 테스트",
                description = "",
                thumbnail = null,
                finishMessage = "",
                rewardSetting = rewardSetting,
                isVisible = true,
                isResultOpen = false,
                sections = listOf(Section.create()),
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
            visitorId = "relay-test-$idx",
        )
}
