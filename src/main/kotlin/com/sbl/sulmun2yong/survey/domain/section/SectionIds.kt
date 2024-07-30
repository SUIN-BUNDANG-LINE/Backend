package com.sbl.sulmun2yong.survey.domain.section

import com.sbl.sulmun2yong.survey.exception.InvalidSectionIdsException

data class SectionIds(
    val sectionIds: List<SectionId>,
) {
    init {
        require(sectionIds.isNotEmpty()) { throw InvalidSectionIdsException() }
        require(sectionIds.first() is SectionId.Standard) { throw InvalidSectionIdsException() }
        require(sectionIds.last() is SectionId.End) { throw InvalidSectionIdsException() }
        require(isSectionIdsUnique()) { throw InvalidSectionIdsException() }
    }

    companion object {
        fun from(standardSectionIds: List<SectionId.Standard>) = SectionIds(standardSectionIds + SectionId.End)
    }

    private fun isSectionIdsUnique() = sectionIds.toSet().size == sectionIds.size

    fun findNextSectionId(sectionId: SectionId.Standard): SectionId {
        val currentIndex = sectionIds.indexOfFirst { it == sectionId }
        return sectionIds[currentIndex + 1]
    }

    fun isContainsAll(sectionIds: List<SectionId>) = sectionIds.all { this.sectionIds.contains(it) }
}
