package com.sbl.sulmun2yong.survey.domain

import java.util.UUID

sealed class NextSectionId {
    abstract val value: UUID?
    abstract val isEnd: Boolean

    companion object {
        fun from(value: UUID?) = if (value == null) End else Standard(value)
    }

    data class Standard(
        val id: UUID,
    ) : NextSectionId() {
        override val value = id
        override val isEnd = false
    }

    data object End : NextSectionId() {
        override val value = null
        override val isEnd = true
    }
}
