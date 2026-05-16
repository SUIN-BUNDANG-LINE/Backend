package com.sbl.sulmun2yong.outbox

import com.sbl.sulmun2yong.IntegrationTest
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.reward.FinishedAt
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.exception.AlreadyParticipatedException
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.ResponseRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import com.sbl.sulmun2yong.survey.service.SurveyResponseService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * F2: 중복 visitorId 응답 제출 시 트랜잭션 원자성 검증
 *
 * 시나리오:
 * 1. 설문 생성 (ImmediateDrawSetting, targetParticipantCount=100)
 * 2. visitorId="duplicate-visitor"로 첫 번째 응답 제출 → 성공 → Outbox 1건
 * 3. 동일 visitorId로 두 번째 응답 제출 → AlreadyParticipatedException
 * 4. 검증: Outbox 여전히 1건 (팬텀 이벤트 방지), 참여자 1명
 */
@IntegrationTest
@Tag("concurrency")
class OutboxAtomicityIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(OutboxAtomicityIntegrationTest::class.java)
        private const val DUPLICATE_VISITOR_ID = "duplicate-visitor"
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

    private lateinit var testSurveyId: UUID
    private lateinit var testSectionId: UUID

    @BeforeEach
    fun setup() {
        val survey = createSurvey()
        surveyRepository.save(survey)
        testSurveyId = survey.id
        testSectionId = survey.sectionEntities.first().id

        log.info("테스트 셋업 완료: surveyId={}", testSurveyId)
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
    fun `중복 visitorId 응답 제출 시 AlreadyParticipatedException 발생하고 Outbox 이벤트 미생성`() {
        // given: 첫 번째 응답 제출 (성공)
        val firstRequest = createResponseRequest(DUPLICATE_VISITOR_ID)
        surveyResponseService.responseToSurvey(testSurveyId, firstRequest)

        val outboxAfterFirst =
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
        assertEquals(1, outboxAfterFirst.size, "첫 번째 응답 제출 후 Outbox 이벤트 1건이어야 함")
        log.info("첫 번째 응답 제출 성공: Outbox 이벤트 {}건", outboxAfterFirst.size)

        // when: 동일 visitorId로 두 번째 응답 제출 → 예외 발생
        val secondRequest = createResponseRequest(DUPLICATE_VISITOR_ID)
        assertThrows(AlreadyParticipatedException::class.java) {
            surveyResponseService.responseToSurvey(testSurveyId, secondRequest)
        }
        log.info("두 번째 응답 제출 시 AlreadyParticipatedException 발생 확인")

        // then 1: Outbox 이벤트가 여전히 1건 (팬텀 이벤트 방지)
        val outboxAfterSecond =
            outboxEventRepository.findAll()
                .filter { it.aggregateType == "Survey" && it.aggregateId == testSurveyId.toString() }
        assertEquals(1, outboxAfterSecond.size, "실패한 두 번째 응답으로 인한 팬텀 Outbox 이벤트가 없어야 함")
        log.info("검증 통과: Outbox 이벤트 {}건 (팬텀 이벤트 없음)", outboxAfterSecond.size)

        // then 2: 참여자 수 1명
        val participantCount = participantRepository.findBySurveyId(testSurveyId).size
        assertEquals(1, participantCount, "참여자는 1명이어야 함")
        log.info("검증 통과: 참여자 수={}", participantCount)
    }

    private fun createSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "테스트", category = "테스트", count = 1)),
                    targetParticipantCount = 100,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            it
                .updateContent(
                    title = "원자성 테스트",
                    description = "",
                    thumbnail = null,
                    finishMessage = "",
                    rewardSetting = rewardSetting,
                    isVisible = true,
                    isResultOpen = false,
                    sections = listOf(Section.create()),
                ).start()
        }

    private fun createResponseRequest(visitorId: String): SurveyResponseRequest =
        SurveyResponseRequest(
            sectionResponses =
                listOf(
                    SurveyResponseRequest.SectionResponseRequest(
                        sectionId = testSectionId,
                        questionResponses = emptyList(),
                    ),
                ),
            visitorId = visitorId,
        )
}
