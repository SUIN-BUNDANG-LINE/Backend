package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import java.util.UUID

@Document(collection = "surveys")
data class SurveyDocument(
    @Id
    val id: UUID,
    var firstSectionId: UUID,
    var status: SurveyStatus,
    var title: String,
    var description: String,
    var thumbnail: String,
    var finishMessage: String,
    var targetParticipants: Int,
    var endDate: Date,
) : BaseTimeDocument()
