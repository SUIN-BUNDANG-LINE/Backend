package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "rewards")
data class RewardDocument(
    @Id
    val id: UUID,
    var surveyId: UUID,
    var name: String,
    var category: String,
    var quantity: Int,
) : BaseTimeDocument()
