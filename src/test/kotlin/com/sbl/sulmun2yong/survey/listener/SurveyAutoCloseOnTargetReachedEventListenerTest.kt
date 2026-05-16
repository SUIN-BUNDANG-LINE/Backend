package com.sbl.sulmun2yong.survey.listener

import com.sbl.sulmun2yong.fixture.survey.SurveyFixtureFactory
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedAutoCloseConsumedEvent
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class SurveyAutoCloseOnTargetReachedEventListenerTest {
    private val surveyRepository: SurveyRepository = mock()
    private val listener = SurveyAutoCloseOnTargetReachedEventListener(surveyRepository)

    @Test
    fun `targetParticipantCount이 null이면 조회하지 않고 종료한다`() {
        listener.handle(
            SurveyResponseSubmittedAutoCloseConsumedEvent(
                surveyId = java.util.UUID.randomUUID().toString(),
                currentParticipantCount = 50,
                targetParticipantCount = null,
            ),
        )

        verify(surveyRepository, never()).findByIdAndIsDeletedFalseWithLock(any())
        verify(surveyRepository, never()).save(any())
    }

    @Test
    fun `목표에 미달하면 종료하지 않는다`() {
        listener.handle(
            SurveyResponseSubmittedAutoCloseConsumedEvent(
                surveyId = java.util.UUID.randomUUID().toString(),
                currentParticipantCount = 50,
                targetParticipantCount = 100,
            ),
        )

        verify(surveyRepository, never()).save(any())
    }

    @Test
    fun `목표 도달 + 진행 중 설문이면 finish하여 저장한다`() {
        val survey = SurveyFixtureFactory.createSurvey(status = SurveyStatus.IN_PROGRESS)
        whenever(surveyRepository.findByIdAndIsDeletedFalseWithLock(survey.id))
            .thenReturn(Optional.of(survey))

        listener.handle(
            SurveyResponseSubmittedAutoCloseConsumedEvent(
                surveyId = survey.id.toString(),
                currentParticipantCount = 100,
                targetParticipantCount = 100,
            ),
        )

        verify(surveyRepository).save(argThat { status == SurveyStatus.CLOSED })
    }

    @Test
    fun `이미 종료된 설문이면 save를 호출하지 않는다 (멱등)`() {
        val survey = SurveyFixtureFactory.createSurvey(status = SurveyStatus.CLOSED)
        whenever(surveyRepository.findByIdAndIsDeletedFalseWithLock(survey.id))
            .thenReturn(Optional.of(survey))

        listener.handle(
            SurveyResponseSubmittedAutoCloseConsumedEvent(
                surveyId = survey.id.toString(),
                currentParticipantCount = 100,
                targetParticipantCount = 100,
            ),
        )

        verify(surveyRepository, never()).save(any())
    }

    @Test
    fun `설문을 찾을 수 없으면 SurveyNotFoundException을 던진다`() {
        val surveyId = java.util.UUID.randomUUID()
        whenever(surveyRepository.findByIdAndIsDeletedFalseWithLock(surveyId))
            .thenReturn(Optional.empty())

        assertThrows<SurveyNotFoundException> {
            listener.handle(
                SurveyResponseSubmittedAutoCloseConsumedEvent(
                    surveyId = surveyId.toString(),
                    currentParticipantCount = 100,
                    targetParticipantCount = 100,
                ),
            )
        }
    }
}
