package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidChoiceException

data class Choices(
    private val choices: List<String>,
) : List<String> by choices {
    init {
        if (choices.isEmpty()) throw InvalidChoiceException()
        if (choices.size != choices.distinct().size) throw InvalidChoiceException()
    }
}
