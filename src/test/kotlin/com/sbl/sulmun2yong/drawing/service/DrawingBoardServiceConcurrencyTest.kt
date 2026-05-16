package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.IntegrationTest
import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.exception.AlreadyParticipatedDrawingException
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.reward.FinishedAt
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.entity.Participant
import com.sbl.sulmun2yong.survey.entity.Survey
import com.sbl.sulmun2yong.survey.repository.ParticipantRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
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

@IntegrationTest
@Tag("concurrency")
class DrawingBoardServiceConcurrencyTest {
    private val log = LoggerFactory.getLogger(DrawingBoardServiceConcurrencyTest::class.java)
    private val logLock = Object()

    @Autowired
    private lateinit var drawingBoardService: DrawingBoardService

    @Autowired
    private lateinit var surveyRepository: SurveyRepository

    @Autowired
    private lateinit var participantRepository: ParticipantRepository

    @Autowired
    private lateinit var drawingBoardRepository: DrawingBoardRepository

    @Autowired
    private lateinit var drawingHistoryRepository: DrawingHistoryRepository

    @Autowired
    private lateinit var transactionTemplate: org.springframework.transaction.support.TransactionTemplate

    private var testSurveyId: UUID = UUID.randomUUID()
    private val testParticipantIds = mutableSetOf<UUID>()

    private fun getNewSurvey(): Survey =
        Survey.create(UUID.randomUUID()).let {
            val rewardSetting =
                ImmediateDrawSetting(
                    rewards = listOf(Reward(name = "아메리카노", category = "커피", count = 50)),
                    targetParticipantCount = 100,
                    finishedAt = FinishedAt(DateUtil.getDateAfterDay(DateUtil.getCurrentDate(noMin = true), 30)),
                )
            testSurveyId = it.id
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

    @BeforeEach
    fun setup() {
        val newSurvey = getNewSurvey()
        surveyRepository.save(newSurvey)

        val drawingBoard =
            DrawingBoard.create(
                surveyId = newSurvey.id,
                boardSize = newSurvey.rewardSetting.targetParticipantCount!!,
                rewards = newSurvey.rewardSetting.rewards,
            )
        drawingBoardRepository.save(drawingBoard)
    }

    @AfterEach
    fun cleanup() {
        transactionTemplate.execute {
            drawingBoardRepository.deleteBySurveyId(testSurveyId)
            surveyRepository.deleteById(testSurveyId)
            testParticipantIds.forEach { participantId ->
                participantRepository.deleteById(participantId)
            }
            drawingHistoryRepository.deleteBySurveyId(testSurveyId)
        }
    }

    @Test
    fun `동시 요청 테스트 - 단 하나의 추첨만 성공`() {
        // given
        val selectedNumber = 5
        val participantInfos =
            (1..10).map {
                val participant =
                    Participant.create(
                        visitorId = UUID.randomUUID().toString(),
                        surveyId = testSurveyId,
                        userId = null,
                    )
                participantRepository.save(participant)
                testParticipantIds.add(participant.id)
                Pair(participant.id, "010-1234-${String.format("%04d", it)}")
            }

        val threadCount = participantInfos.size
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val results = mutableListOf<String>()

        // when
        participantInfos.forEach { (participantId, phoneNumber) ->
            executor.submit {
                startLatch.await()
                try {
                    drawingBoardService.doDrawing(participantId, selectedNumber, phoneNumber)
                    synchronized(results) {
                        results.add("success: ($participantId, $phoneNumber)")
                    }
                } catch (e: Exception) {
                    val threadId = Thread.currentThread().id
                    synchronized(logLock) {
                        log.info(
                            "[Thread-{}] 추첨 실패 - 참가자: {}, 전화번호: {}, 에러: {}",
                            threadId,
                            participantId,
                            phoneNumber,
                            e.message,
                        )
                    }
                    synchronized(results) {
                        results.add("failure: ($participantId, $phoneNumber): ${e.message}")
                    }

                    // AlreadySelectedTicketException이 아닌 다른 예외가 발생하면 테스트 실패
                    if (e !is AlreadySelectedTicketException) {
                        throw e
                    }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        // then
        val successCount = results.count { it.startsWith("success") }
        assertEquals(1, successCount, "동시에 한 요청만 성공해야 합니다. 결과: $results")

        // 실패한 요청들은 모두 AlreadySelectedTicketException이어야 함
        val failureResults = results.filter { it.startsWith("failure") }
        assertTrue(
            failureResults.all { it.contains("이미 선택된 티켓") },
            "실패한 요청들은 모두 AlreadySelectedTicketException이어야 합니다. 결과: $failureResults",
        )
    }

    @Test
    fun `동일 사용자 동시 요청 테스트 - 단 하나의 추첨만 성공`() {
        // given
        val participant =
            Participant.create(
                visitorId = UUID.randomUUID().toString(),
                surveyId = testSurveyId,
                userId = null,
            )
        participantRepository.save(participant)
        testParticipantIds.add(participant.id)
        val phoneNumber = "010-1234-5678"

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val results = mutableListOf<String>()

        // when
        repeat(threadCount) { index ->
            executor.submit {
                startLatch.await()
                try {
                    // 각 스레드마다 다른 추첨권 번호 선택 (1~10)
                    val selectedNumber = index + 1
                    drawingBoardService.doDrawing(participant.id, selectedNumber, phoneNumber)
                    synchronized(results) {
                        results.add("success: ($participant.id, $phoneNumber) - 선택번호: $selectedNumber")
                    }
                } catch (e: Exception) {
                    val threadId = Thread.currentThread().id
                    synchronized(logLock) {
                        log.info(
                            "[Thread-{}] 추첨 실패 - 참가자: {}, 전화번호: {}, 에러: {}",
                            threadId,
                            participant.id,
                            phoneNumber,
                            e.message,
                        )
                    }
                    synchronized(results) {
                        results.add("failure: ($participant.id, $phoneNumber): ${e.message}")
                    }

                    // AlreadyParticipatedDrawingException이 아닌 다른 예외가 발생하면 테스트 실패
                    if (e !is AlreadyParticipatedDrawingException) {
                        throw e
                    }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        // then
        val successCount = results.count { it.startsWith("success") }
        assertEquals(1, successCount, "동시에 한 요청만 성공해야 합니다. 결과: $results")

        // 실패한 요청들은 모두 AlreadyParticipatedDrawingException이어야 함
        val failureResults = results.filter { it.startsWith("failure") }
        assertTrue(
            failureResults.all { it.contains("이미 참여한 추첨입니다.") },
            "실패한 요청들은 모두 '이미 참여한 추첨입니다.' 메시지를 포함해야 합니다. 결과: $failureResults",
        )
    }

    @Test
    fun `여러 사용자가 서로 다른 번호로 동시 추첨 테스트`() {
        // given
        val participantInfos =
            (1..10).map {
                val participant =
                    Participant.create(
                        visitorId = UUID.randomUUID().toString(),
                        surveyId = testSurveyId,
                        userId = null,
                    )
                participantRepository.save(participant)
                testParticipantIds.add(participant.id)
                Triple(
                    participant.id,
                    "010-1234-${String.format("%04d", it)}",
                    it,
                ) // participantId, phoneNumber, selectedNumber
            }

        val threadCount = participantInfos.size
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val results = mutableListOf<String>()

        // when
        participantInfos.forEach { (participantId, phoneNumber, selectedNumber) ->
            executor.submit {
                startLatch.await()
                try {
                    drawingBoardService.doDrawing(participantId, selectedNumber, phoneNumber)
                    val threadId = Thread.currentThread().id
                    synchronized(logLock) {
                        log.info(
                            "[Thread-{}] 추첨 성공 - 참가자: {}, 전화번호: {}, 선택번호: {}",
                            threadId,
                            participantId,
                            phoneNumber,
                            selectedNumber,
                        )
                    }
                    synchronized(results) {
                        results.add("success: ($participantId, $phoneNumber) - 선택번호: $selectedNumber")
                    }
                } catch (e: Exception) {
                    val threadId = Thread.currentThread().id
                    synchronized(logLock) {
                        log.info(
                            "[Thread-{}] 추첨 실패 - 참가자: {}, 전화번호: {}, 선택번호: {}, 에러: {}",
                            threadId,
                            participantId,
                            phoneNumber,
                            selectedNumber,
                            e.message,
                        )
                    }
                    synchronized(results) {
                        results.add("failure: ($participantId, $phoneNumber) - 선택번호: $selectedNumber: ${e.message}")
                    }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        // then
        val successResults = results.filter { it.startsWith("success") }
        val failureResults = results.filter { it.startsWith("failure") }

        // 모든 요청이 성공해야 함
        assertEquals(10, successResults.size, "모든 추첨이 성공해야 합니다. 결과: $results")
        assertEquals(0, failureResults.size, "실패한 추첨이 없어야 합니다. 결과: $results")
    }
}
