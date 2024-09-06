package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.ResponseAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyParticipantResponse
import com.sbl.sulmun2yong.survey.exception.AlreadyParticipatedException
import com.sbl.sulmun2yong.survey.exception.SurveyClosedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyResponseService(
    val surveyAdapter: SurveyAdapter,
    val participantAdapter: ParticipantAdapter,
    val responseAdapter: ResponseAdapter,
) {
    // TODO: 트랜잭션 처리 추가하기
    fun responseToSurvey(
        surveyId: UUID,
        surveyResponseRequest: SurveyResponseRequest,
        isAdmin: Boolean,
    ): SurveyParticipantResponse {
        // 이미 참여한 설문인지 검증(Admin인 경우 스킵)
        if (!isAdmin) validateIsAlreadyParticipated(surveyId)

        val survey = surveyAdapter.getSurvey(surveyId)
        if (survey.status != SurveyStatus.IN_PROGRESS) {
            throw SurveyClosedException()
        }
        val surveyResponse = surveyResponseRequest.toDomain(surveyId)
        survey.validateResponse(surveyResponse)
        // TODO: 참가자 객체의 UserId에 실제 유저 값 넣기
        val participant = Participant.create(surveyId, null)
        participantAdapter.insert(participant)
        responseAdapter.insertSurveyResponse(surveyResponse, participant.id)
        return SurveyParticipantResponse(participant.id, survey.isImmediateDraw())
    }

    private fun validateIsAlreadyParticipated(surveyId: UUID) {
        val participant = participantAdapter.findBySurveyId(surveyId)
        participant?.let {
            throw AlreadyParticipatedException()
        }
    }
}
