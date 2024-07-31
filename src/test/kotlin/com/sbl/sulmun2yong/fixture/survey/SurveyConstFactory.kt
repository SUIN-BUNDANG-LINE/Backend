package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.question.choice.Choice

object SurveyConstFactory {
    /** listOf(“a”, “b”, “c”) */
    val CONTENTS = listOf("a", "b", "c")

    /** 제목 */
    const val TITLE = "제목"

    /** a,b,c,기타 */
    val CHOICE_SET =
        setOf(
            Choice.Standard("a"),
            Choice.Standard("b"),
            Choice.Standard("c"),
            Choice.Other,
        )
}
