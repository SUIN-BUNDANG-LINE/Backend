package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import java.util.UUID

// TODO: Section 추가하기
@Document(collection = "surveys")
data class SurveyDocument(
    @Id
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String,
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipants: Int?,
    val rewards: List<RewardSubDocument>,
) : BaseTimeDocument() {
    data class RewardSubDocument(
        val rewardId: UUID,
        val name: String,
        val category: String,
        val count: Int,
    )
}
