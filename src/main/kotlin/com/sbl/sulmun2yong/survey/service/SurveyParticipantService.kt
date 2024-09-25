package com.sbl.sulmun2yong.survey.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingHistoryAdapter
import com.sbl.sulmun2yong.survey.adapter.ParticipantAdapter
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import com.sbl.sulmun2yong.survey.dto.response.ParticipantsInfoListResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SurveyParticipantService(
    private val surveyAdapter: SurveyAdapter,
    private val participantAdapter: ParticipantAdapter,
    private val drawingHistoryAdapter: DrawingHistoryAdapter,
) {
    fun getSurveyParticipants(
        surveyId: UUID,
        makerId: UUID,
    ): ParticipantsInfoListResponse {
        val survey = surveyAdapter.getByIdAndMakerId(surveyId, makerId)
        val participants = participantAdapter.findBySurveyId(surveyId)
        val drawingHistories = if (survey.isImmediateDraw()) drawingHistoryAdapter.getBySurveyId(surveyId, false) else null
        return ParticipantsInfoListResponse.of(participants, drawingHistories)
    }
}
