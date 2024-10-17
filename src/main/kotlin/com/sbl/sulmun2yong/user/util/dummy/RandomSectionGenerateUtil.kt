package com.sbl.sulmun2yong.user.util.dummy

import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.user.util.dummy.RandomQuestionGenerateUtil.generateRandomQuestion

object RandomSectionGenerateUtil {
    private val randomQuestionCountPicker =
        ProbabilityPicker(
            mapOf(
                1 to 0.1,
                2 to 0.1,
                3 to 0.15,
                4 to 0.15,
                5 to 0.15,
                6 to 0.15,
                7 to 0.1,
                8 to 0.1,
            ),
        )

    fun generateRandomSection(
        index: Int,
        id: SectionId.Standard,
        sectionIds: SectionIds,
    ): Section {
        val title = "${index + 1}번 섹션"
        val description = "${index + 1}번 섹션 설명"
        val questionCount = randomQuestionCountPicker.pick()

        return Section(
            id = id,
            title = title,
            description = description,
            // 편의상 NumericalOrder로 고정
            routingStrategy = RoutingStrategy.NumericalOrder,
            questions = (0 until questionCount).map { generateRandomQuestion(it) },
            sectionIds = sectionIds,
        )
    }
}
