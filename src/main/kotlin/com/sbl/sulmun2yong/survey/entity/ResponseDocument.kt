package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "responses")
data class ResponseDocument(
    @Id
    val id: UUID,
    val participantId: UUID,
    val questionId: UUID,
    val content: String,
) : BaseTimeDocument()
