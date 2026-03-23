package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardMultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardSingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.impl.StandardTextQuestion
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
@Table(name = "questions")
class QuestionEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    val section: SectionEntity,
    @Column(nullable = false)
    val orderIndex: Int,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(nullable = false)
    val isRequired: Boolean,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: QuestionType,
    @Column(nullable = false)
    val isAllowOther: Boolean,
    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    val choices: MutableList<ChoiceEntity> = mutableListOf(),
) {
    companion object {
        fun from(
            question: Question,
            section: SectionEntity,
            orderIndex: Int,
        ): QuestionEntity {
            val entity =
                QuestionEntity(
                    id = question.id,
                    section = section,
                    orderIndex = orderIndex,
                    title = question.title,
                    description = question.description,
                    isRequired = question.isRequired,
                    type = question.questionType,
                    isAllowOther =
                        when (question) {
                            is StandardSingleChoiceQuestion -> question.choices.isAllowOther
                            is StandardMultipleChoiceQuestion -> question.choices.isAllowOther
                            else -> false
                        },
                )
            // 선택지 추가
            when (question) {
                is StandardSingleChoiceQuestion ->
                    question.choices.standardChoices.forEachIndexed { index, choice ->
                        entity.choices.add(ChoiceEntity(question = entity, orderIndex = index, content = choice.content))
                    }
                is StandardMultipleChoiceQuestion ->
                    question.choices.standardChoices.forEachIndexed { index, choice ->
                        entity.choices.add(ChoiceEntity(question = entity, orderIndex = index, content = choice.content))
                    }
                else -> {}
            }
            return entity
        }
    }

    fun toDomain(): Question =
        when (type) {
            QuestionType.SINGLE_CHOICE ->
                StandardSingleChoiceQuestion(
                    id = id,
                    title = title,
                    description = description,
                    isRequired = isRequired,
                    choices = Choices(choices.map { Choice.Standard(it.content) }, isAllowOther),
                )
            QuestionType.MULTIPLE_CHOICE ->
                StandardMultipleChoiceQuestion(
                    id = id,
                    title = title,
                    description = description,
                    isRequired = isRequired,
                    choices = Choices(choices.map { Choice.Standard(it.content) }, isAllowOther),
                )
            QuestionType.TEXT_RESPONSE ->
                StandardTextQuestion(
                    id = id,
                    title = title,
                    description = description,
                    isRequired = isRequired,
                )
        }
}
