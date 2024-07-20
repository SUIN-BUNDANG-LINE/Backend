package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.exception.InvalidSectionRouteConfigsException
import java.util.UUID

data class SectionRouteConfigs(
    val sectionRouteConfigs: List<SectionRouteConfig>,
) : List<SectionRouteConfig> by sectionRouteConfigs {
    private val contentSet = sectionRouteConfigs.map { it.content }.toSet()

    init {
        require(sectionRouteConfigs.isNotEmpty()) { throw InvalidSectionRouteConfigsException() }
        require(isConfigsUnique()) { throw InvalidSectionRouteConfigsException() }
    }

    fun findByContent(content: String?) = sectionRouteConfigs.find { it.content == content }

    private fun isConfigsUnique() = sectionRouteConfigs.size == contentSet.size
}

/**
 * 응답에 따른 다음 섹션의 ID를 저장하는 클래스
 * @property content 응답의 내용, null이면 기타 응답
 * @property nextSectionId 다음 섹션의 ID, null이면 설문 종료
 */
data class SectionRouteConfig(
    val content: String?,
    val nextSectionId: UUID?,
)
