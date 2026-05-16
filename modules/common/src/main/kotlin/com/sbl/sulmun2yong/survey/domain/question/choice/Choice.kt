package com.sbl.sulmun2yong.survey.domain.question.choice

/** 선택지(Standard: 일반 선택지, Other: 기타 선택지) */
sealed class Choice {
    abstract val content: String?

    companion object {
        fun from(content: String?) = if (content == null) Other else Standard(content)
    }

    /** 일반 선택지 */
    data class Standard(
        override val content: String,
    ) : Choice()

    /** 기타 선택지 */
    data object Other : Choice() {
        override val content = null
    }
}
