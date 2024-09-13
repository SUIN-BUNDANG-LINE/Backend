package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.global.util.DateUtil
import com.sbl.sulmun2yong.survey.domain.response.SurveyResponse
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.domain.reward.NoRewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.RewardSetting
import com.sbl.sulmun2yong.survey.domain.reward.SelfManagementSetting
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyStartException
import com.sbl.sulmun2yong.survey.exception.InvalidUpdateSurveyException
import java.util.Date
import java.util.UUID

data class Survey(
    val id: UUID,
    val title: String,
    val description: String,
    val thumbnail: String?,
    val publishedAt: Date?,
    val status: SurveyStatus,
    val finishMessage: String,
    val rewardSetting: RewardSetting,
    /** 해당 설문의 설문이용 노출 여부(false면 메인 페이지 노출 X, 링크를 통해서만 접근 가능) */
    val isVisible: Boolean,
    val makerId: UUID,
    val sections: List<Section>,
) {
    init {
        require(sections.isNotEmpty()) { throw InvalidSurveyException() }
        require(isSectionsUnique()) { throw InvalidSurveyException() }
        require(isSurveyStatusValid()) { throw InvalidSurveyException() }
        require(isFinishedAtAfterPublishedAt()) { throw InvalidSurveyException() }
        require(isSectionIdsValid()) { throw InvalidSurveyException() }
    }

    companion object {
        const val DEFAULT_THUMBNAIL_URL = "https://test-oriddle-bucket.s3.ap-northeast-2.amazonaws.com/surveyImage.webp"
        const val DEFAULT_TITLE = "제목 없는 설문"
        const val DEFAULT_DESCRIPTION = ""
        const val DEFAULT_FINISH_MESSAGE = "설문에 참여해주셔서 감사합니다."

        fun create(makerId: UUID) =
            Survey(
                id = UUID.randomUUID(),
                title = DEFAULT_TITLE,
                description = DEFAULT_DESCRIPTION,
                thumbnail = null,
                publishedAt = null,
                status = SurveyStatus.NOT_STARTED,
                finishMessage = DEFAULT_FINISH_MESSAGE,
                rewardSetting = NoRewardSetting,
                isVisible = true,
                makerId = makerId,
                sections = listOf(Section.create()),
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
        finishMessage: String,
        rewardSetting: RewardSetting,
        isVisible: Boolean,
        sections: List<Section>,
    ): Survey {
        // 설문이 시작 전 상태이거나, 수정 중이면서 리워드 관련 정보가 변경되지 않아야한다.
        require(
            status == SurveyStatus.NOT_STARTED ||
                status == SurveyStatus.IN_MODIFICATION &&
                rewardSetting == this.rewardSetting,
        ) {
            throw InvalidUpdateSurveyException()
        }
        return copy(
            title = title,
            description = description,
            thumbnail = thumbnail,
            finishMessage = finishMessage,
            rewardSetting = rewardSetting,
            isVisible = isVisible,
            sections = sections,
        )
    }

    fun finish() = copy(status = SurveyStatus.CLOSED)

    /** 설문을 IN_PROGRESS 상태로 변경하는 메서드. 설문이 시작 전이거나 수정 중인 경우만 가능하다. */
    fun start() =
        when (status) {
            SurveyStatus.NOT_STARTED -> copy(status = SurveyStatus.IN_PROGRESS, publishedAt = DateUtil.getCurrentDate())
            SurveyStatus.IN_MODIFICATION -> copy(status = SurveyStatus.IN_PROGRESS)
            SurveyStatus.IN_PROGRESS -> throw InvalidSurveyStartException()
            SurveyStatus.CLOSED -> throw InvalidSurveyStartException()
        }

    fun edit(): Survey {
        require(status == SurveyStatus.IN_PROGRESS) { throw InvalidSurveyEditException() }
        return copy(status = SurveyStatus.IN_MODIFICATION)
    }

    fun isImmediateDraw() = rewardSetting.isImmediateDraw

    private fun isSectionsUnique() = sections.size == sections.distinctBy { it.id }.size

    private fun isSurveyStatusValid() = publishedAt != null || status == SurveyStatus.NOT_STARTED

    private fun isFinishedAtAfterPublishedAt(): Boolean {
        if (publishedAt == null) return true
        if (rewardSetting is SelfManagementSetting) return rewardSetting.finishedAt.value.after(publishedAt)
        if (rewardSetting is ImmediateDrawSetting) return rewardSetting.finishedAt.value.after(publishedAt)
        return true
    }

    private fun isSectionIdsValid(): Boolean {
        val sectionIds = SectionIds.from(sections.map { it.id })
        return sections.all { it.sectionIds == sectionIds }
    }
}
