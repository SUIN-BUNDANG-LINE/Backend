package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.SectionRouteType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "sections")
data class SectionDocument(
    @Id
    val id: UUID,
    var surveyId: UUID,
    var routeType: SectionRouteType,
    var title: String,
    var description: String,
    var number: Int,
    var nextSectionId: UUID? = null, // Optional, only for SET_BY_USER
    var nextSectionConfigs: List<NextSectionConfig>? = null, // Optional, only for SET_BY_CHOICE
) : BaseTimeDocument()

data class NextSectionConfig(
    var questionId: UUID,
    var nextSectionId: UUID,
    var optionContent: String,
)
