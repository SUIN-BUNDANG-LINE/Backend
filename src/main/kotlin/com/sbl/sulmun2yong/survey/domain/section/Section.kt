package com.sbl.sulmun2yong.survey.domain.section

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

data class Section(
    val id: SectionId.Standard,
    val title: String,
    val description: String,
    val routeDetails: RouteDetails,
    val questions: List<Question>,
    val sectionIds: SectionIds,
) {
    init {
        validateSection()
    }

    private fun validateSection() {
        // 섹션 라우팅 방식이 선택지 기반이면 아래의 유효성 검증을 진행
        if (routeDetails is RouteDetails.SetByChoiceRouting) {
            // keyQuestionId에 해당하는 질문이 섹션 내에 없으면 예외 발생
            val keyQuestion = findKeyQuestion(routeDetails.keyQuestionId) ?: throw InvalidSectionException()
            // 라우팅 설정의 선택지들과 keyQuestion의 선택지가 일치해야함
            require(keyQuestion.getChoiceSet() == routeDetails.getChoiceSet()) { throw InvalidSectionException() }
        }
        // 섹션 라우팅으로 나올 수 있는 섹션들이 모두 설문 내에 존재하는 섹션이여야함
        require(sectionIds.isContainsAll(routeDetails.getNextSectionIds())) { throw InvalidSectionException() }
    }

    fun findNextSectionId(sectionResponse: SectionResponse): SectionId {
        validateResponse(sectionResponse)
        return when (routeDetails) {
            is RouteDetails.SetByUserRouting -> routeDetails.nextSectionId
            is RouteDetails.SetByChoiceRouting -> routeDetails.findNextSectionId(sectionResponse)
            is RouteDetails.NumericalOrderRouting -> sectionIds.findNextSectionId(sectionResponse.sectionId)
        }
    }

    private fun findKeyQuestion(keyQuestionId: UUID) =
        questions.find { it.id == keyQuestionId && it.canBeKeyQuestion() } as? SingleChoiceQuestion

    private fun validateResponse(sectionResponse: SectionResponse) {
        questions.forEach { question ->
            val response = sectionResponse.find { it.questionId == question.id }
            // 질문에 해당하는 응답이 없는데, 필수 질문이라면 예외 발생
            if (response == null && question.isRequired) throw InvalidSectionResponseException()
            // 질문에 해당하는 응답이 있는데, 유효한 응답이 아니라면 예외 발생
            if (response != null && !question.isValidResponse(response)) throw InvalidSectionResponseException()
        }
    }
}
