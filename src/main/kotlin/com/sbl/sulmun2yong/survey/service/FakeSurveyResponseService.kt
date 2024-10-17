package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.global.util.FakeFingerprintApi
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.ResponseAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.domain.Participant
import com.sbl.sulmun2yong.survey.dto.request.SurveyResponseRequest
import com.sbl.sulmun2yong.survey.dto.response.SurveyParticipantResponse
import com.sbl.sulmun2yong.survey.exception.AlreadyParticipatedException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

@ConditionalOnProperty(prefix = "fingerprint", name = ["mocking-server-url"], matchIfMissing = false)
@Service
class FakeSurveyResponseService(
    val surveyAdapter: SurveyAdapter,
    val participantAdapter: ParticipantAdapter,
    val responseAdapter: ResponseAdapter,
    val fakeFingerprintApi: FakeFingerprintApi,
) {
    fun fakeResponseToSurvey(
        surveyId: UUID,
        surveyResponseRequest: SurveyResponseRequest,
    ): SurveyParticipantResponse {
        validateIsAlreadyParticipated(surveyId, surveyResponseRequest.visitorId)
        // 가짜 Fingerprint API를 호출하는 부분을 제외하고는 SurveyResponseService responseToSurvey 동일
        fakeFingerprintApi.callFingerPrintApi(surveyResponseRequest.visitorId)
        val visitorId = surveyResponseRequest.visitorId
        val survey = surveyAdapter.getSurvey(surveyId)
        val surveyResponse = surveyResponseRequest.toDomain(surveyId)
        survey.validateResponse(surveyResponse)
        val participant = Participant.create(visitorId, surveyId, null)
        participantAdapter.insert(participant)
        responseAdapter.insertSurveyResponse(surveyResponse, participant.id)
        return SurveyParticipantResponse(participant.id, survey.isImmediateDraw())
    }

    private fun validateIsAlreadyParticipated(
        surveyId: UUID,
        visitorId: String,
    ) {
        val participant = participantAdapter.findBySurveyIdAndVisitorId(surveyId, visitorId)
        participant?.let {
            throw AlreadyParticipatedException()
        }
    }
}
