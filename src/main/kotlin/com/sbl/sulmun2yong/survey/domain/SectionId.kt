package com.sbl.sulmun2yong.survey.domain

import java.util.UUID

sealed class SectionId {
    abstract val value: UUID?
    abstract val isEnd: Boolean

    companion object {
        fun from(value: UUID?) = if (value == null) End else Standard(value)
    }

    data class Standard(
        override val value: UUID,
    ) : SectionId() {
        override val isEnd = false
    }

    data object End : SectionId() {
        override val value = null
        override val isEnd = true
    }
}
