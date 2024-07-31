package com.sbl.sulmun2yong.survey.domain.section

import java.util.UUID

/** 섹션의 ID(Standard: 일반적인 섹션의 ID, End: 마지막 섹션) */
sealed class SectionId {
    abstract val value: UUID?

    companion object {
        fun from(value: UUID?) = if (value == null) End else Standard(value)
    }

    /** 일반적인 섹션의 ID */
    data class Standard(
        override val value: UUID,
    ) : SectionId()

    /** 마지막 섹션 */
    data object End : SectionId() {
        override val value = null
    }
}
