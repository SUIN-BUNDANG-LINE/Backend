package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.routing.RoutingType
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "sections")
class SectionEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    val survey: SurveyEntity,
    @Column(nullable = false)
    val orderIndex: Int,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val routeType: RoutingType,
    // SetByUser 전략일 때 다음 섹션 ID
    @Column(columnDefinition = "BINARY(16)")
    val nextSectionId: UUID?,
    // SetByChoice 전략일 때 기준 질문 ID
    @Column(columnDefinition = "BINARY(16)")
    val keyQuestionId: UUID?,
    @OneToMany(mappedBy = "section", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val sectionRouteConfigs: MutableList<SectionRouteConfigEntity> = mutableListOf(),
    @OneToMany(mappedBy = "section", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val questions: MutableList<QuestionEntity> = mutableListOf(),
) {
    companion object {
        fun from(
            section: Section,
            survey: SurveyEntity,
            orderIndex: Int,
        ): SectionEntity {
            val entity =
                SectionEntity(
                    id = section.id.value,
                    survey = survey,
                    orderIndex = orderIndex,
                    title = section.title,
                    description = section.description,
                    routeType = section.routingStrategy.type,
                    nextSectionId =
                        when (section.routingStrategy) {
                            is RoutingStrategy.SetByUser -> section.routingStrategy.nextSectionId.value
                            else -> null
                        },
                    keyQuestionId =
                        when (section.routingStrategy) {
                            is RoutingStrategy.SetByChoice -> section.routingStrategy.keyQuestionId
                            else -> null
                        },
                )
            // 라우팅 설정
            if (section.routingStrategy is RoutingStrategy.SetByChoice) {
                section.routingStrategy.routingMap.entries.forEachIndexed { index, (choice, sectionId) ->
                    entity.sectionRouteConfigs.add(
                        SectionRouteConfigEntity(
                            section = entity,
                            orderIndex = index,
                            choiceContent = choice.content,
                            nextSectionId = sectionId.value,
                        ),
                    )
                }
            }
            // 질문
            section.questions.forEachIndexed { index, question ->
                entity.questions.add(QuestionEntity.from(question, entity, index))
            }
            return entity
        }
    }

    fun toDomain(sectionIds: SectionIds): Section {
        val routingStrategy =
            when (routeType) {
                RoutingType.NUMERICAL_ORDER -> RoutingStrategy.NumericalOrder
                RoutingType.SET_BY_USER -> RoutingStrategy.SetByUser(SectionId.from(nextSectionId))
                RoutingType.SET_BY_CHOICE ->
                    RoutingStrategy.SetByChoice(
                        keyQuestionId = keyQuestionId!!,
                        routingMap =
                            sectionRouteConfigs.associate {
                                Choice.from(it.choiceContent) to SectionId.from(it.nextSectionId)
                            },
                    )
            }
        return Section(
            id = SectionId.Standard(id),
            title = title,
            description = description,
            routingStrategy = routingStrategy,
            questions = questions.map { it.toDomain() },
            sectionIds = sectionIds,
        )
    }
}
