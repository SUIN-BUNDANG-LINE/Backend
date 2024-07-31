package com.sbl.sulmun2yong.survey.domain.section

import java.util.UUID

sealed class SectionId {
    abstract val value: UUID?

    companion object {
        fun from(value: UUID?) = if (value == null) End else Standard(value)
    }

    data class Standard(
        override val value: UUID,
    ) : SectionId()

    data object End : SectionId() {
        override val value = null
    }
}
