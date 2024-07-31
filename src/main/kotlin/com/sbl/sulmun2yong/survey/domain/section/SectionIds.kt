package com.sbl.sulmun2yong.survey.domain.section

import com.sbl.sulmun2yong.survey.exception.InvalidSectionIdsException

/** 설문에 속한 섹션들의 ID를 관리하는 클래스 */
data class SectionIds(
    val sectionIds: List<SectionId>,
) {
    init {
        require(sectionIds.isNotEmpty()) { throw InvalidSectionIdsException() }
        // 첫 섹션 ID는 Standard
        require(sectionIds.first() is SectionId.Standard) { throw InvalidSectionIdsException() }
        // 마지막 섹션 ID는 End
        require(sectionIds.last() is SectionId.End) { throw InvalidSectionIdsException() }
        require(isSectionIdsUnique()) { throw InvalidSectionIdsException() }
    }

    companion object {
        fun from(standardSectionIds: List<SectionId.Standard>) = SectionIds(standardSectionIds + SectionId.End)
    }

    private fun isSectionIdsUnique() = sectionIds.toSet().size == sectionIds.size

    /** 다음 인덱스의 섹션 ID를 찾는다 */
    fun findNextSectionId(sectionId: SectionId.Standard): SectionId {
        val currentIndex = sectionIds.indexOfFirst { it == sectionId }
        return sectionIds[currentIndex + 1]
    }

    fun isContainsAll(sectionIds: List<SectionId>) = sectionIds.all { this.sectionIds.contains(it) }
}
