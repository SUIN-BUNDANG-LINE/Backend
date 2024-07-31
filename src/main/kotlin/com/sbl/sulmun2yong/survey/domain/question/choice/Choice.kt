package com.sbl.sulmun2yong.survey.domain.question.choice

sealed class Choice {
    abstract val content: String?

    companion object {
        fun from(content: String?) = if (content == null) Other else Standard(content)
    }

    data class Standard(
        override val content: String,
    ) : Choice()

    data object Other : Choice() {
        override val content = null
    }
}
