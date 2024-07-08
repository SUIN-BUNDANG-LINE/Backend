package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.QuestionType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "questions")
data class QuestionDocument(
    @Id
    val id: UUID,
    var sectionId: UUID,
    var title: String,
    var description: String,
    var type: QuestionType,
    var isRequired: Boolean,
    var isAllowOther: Boolean? = null, // Optional, true for SINGLE_CHOICE and MULTIPLE_CHOICE if 'other' is allowed
    var choices: List<String>? = null, // Optional for SINGLE_CHOICE and MULTIPLE_CHOICE
) : BaseTimeDocument()
