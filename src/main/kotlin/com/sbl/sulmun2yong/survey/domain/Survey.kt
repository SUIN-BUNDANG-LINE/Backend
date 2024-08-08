package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import com.sbl.sulmun2yong.survey.exception.InvalidUpdateSurveyException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

// TODO: 설문 일정 관련 속성들을 하나의 클래스로 묶기
data class Survey(
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String?,
    val publishedAt: Date?,
    val finishedAt: Date,
    val status: SurveyStatus,
    val finishMessage: String,
    val targetParticipantCount: Int,
    val makerId: UUID,
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

    companion object {
        const val DEFAULT_THUMBNAIL_URL = "https://test-oriddle-bucket.s3.ap-northeast-2.amazonaws.com/surveyImage.webp"
        const val DEFAULT_TITLE = "제목 없는 설문"
        const val DEFAULT_DESCRIPTION = ""
        const val DEFAULT_SURVEY_DURATION = 60L
        const val DEFAULT_FINISH_MESSAGE = "설문에 참여해주셔서 감사합니다."
        const val DEFAULT_TARGET_PARTICIPANT_COUNT = 100

        fun create(makerId: UUID) =
            Survey(
                id = UUID.randomUUID(),
                title = DEFAULT_TITLE,
                description = DEFAULT_DESCRIPTION,
                thumbnail = null,
                publishedAt = null,
                finishedAt = getDefaultFinishedAt(),
                status = SurveyStatus.NOT_STARTED,
                finishMessage = DEFAULT_FINISH_MESSAGE,
                targetParticipantCount = DEFAULT_TARGET_PARTICIPANT_COUNT,
                makerId = makerId,
                rewards = emptyList(),
                sections = listOf(Section.create()),
            )

        /** 설문 종료일 기본 값은 현재 날짜 기준 60일 뒤, 초 단위 이하 제거 */
        private fun getDefaultFinishedAt() =
            Date.from(
                LocalDateTime
                    .now()
                    .plusDays(DEFAULT_SURVEY_DURATION)
                    .withSecond(0)
                    .withNano(0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
            )
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

    /** 받은 값으로 수정한 설문을 반환하는 메서드 */
    fun updateContent(
        title: String,
        description: String,
        thumbnail: String?,
        finishedAt: Date,
        finishMessage: String,
        targetParticipantCount: Int,
        rewards: List<Reward>,
        sections: List<Section>,
    ): Survey {
        // 설문이 시작 전 상태이거나, 수정 중이면서 리워드 관련 정보가 변경되지 않아야한다.
        require(
            status == SurveyStatus.NOT_STARTED ||
                status == SurveyStatus.IN_MODIFICATION &&
                isRewardInfoEquals(targetParticipantCount, rewards),
        ) {
            throw InvalidUpdateSurveyException()
        }
        return copy(
            title = title,
            description = description,
            thumbnail = thumbnail,
            finishedAt = finishedAt,
            finishMessage = finishMessage,
            targetParticipantCount = targetParticipantCount,
            rewards = rewards,
            sections = sections,
        )
    }

    /** 리워드 관련 정보가 같은지 확인하는 메서드 */
    private fun isRewardInfoEquals(
        targetParticipantCount: Int,
        rewards: List<Reward>,
    ) = targetParticipantCount == this.targetParticipantCount && rewards == this.rewards

    fun finish() = copy(status = SurveyStatus.CLOSED)

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
