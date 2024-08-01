package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import java.util.Date
import java.util.UUID

// TODO: 설문 일정 관련 속성들을 하나의 클래스로 묶기
data class Survey(
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String,
    val publishedAt: Date?,
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipantCount: Int,
    val rewards: List<Reward>,
    val sections: List<Section>,
) {
    init {
        require(sections.isNotEmpty()) { throw InvalidSurveyException() }
        require(isSectionsUnique()) { throw InvalidSurveyException() }
        require(isSurveyStatusValid()) { throw InvalidSurveyException() }
        require(isFinishedAtAfterPublishedAt()) { throw InvalidSurveyException() }
        require(isTargetParticipantsEnough()) { throw InvalidSurveyException() }
        require(isSectionIdsValid()) { throw InvalidSurveyException() }
    }

    /** 설문의 응답 순서가 유효한지, 응답이 각 섹션에 유효한지 확인하는 메서드 */
    fun validateResponse(surveyResponse: SurveyResponse) {
        // 확인할 응답의 예상 섹션 ID, 첫 응답의 섹션 ID는 첫 섹션의 ID
        var expectedSectionId: SectionId = sections.first().id
        for (sectionResponse in surveyResponse) {
            val responseSectionId = sectionResponse.sectionId
            require(expectedSectionId == responseSectionId) { throw InvalidSurveyResponseException() }
            // section.findNextSectionId()는 무조건 설문에 속한 섹션 ID를 반환하므로 responseSectionId에 해당하는 섹션은 무조건 존재함
            val section = sections.first { it.id == responseSectionId }
            // 다음 섹션 ID를 찾아서 예상 섹션 ID로 설정
            expectedSectionId = section.findNextSectionId(sectionResponse)
        }
        // 모든 응답을 확인한 뒤 예상 섹션 ID가 종료 섹션 ID인지 확인
        require(expectedSectionId is SectionId.End) { throw InvalidSurveyResponseException() }
    }

    fun getRewardCount() = rewards.sumOf { it.count }

    private fun isSectionsUnique() = sections.size == sections.distinctBy { it.id }.size

    private fun isSurveyStatusValid() = publishedAt != null || status == SurveyStatus.NOT_STARTED

    private fun isFinishedAtAfterPublishedAt() = publishedAt == null || finishedAt.after(publishedAt)

    private fun isTargetParticipantsEnough() = targetParticipantCount >= getRewardCount()

    private fun isSectionIdsValid(): Boolean {
        val sectionIds = SectionIds.from(sections.map { it.id })
        return sections.all { it.sectionIds == sectionIds }
    }
}
