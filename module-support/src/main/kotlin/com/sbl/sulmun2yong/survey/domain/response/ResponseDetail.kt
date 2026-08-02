package com.sbl.sulmun2yong.survey.domain.response

/**
 * 설문 응답 상세
 * @property content 응답 내용
 * @property isOther 기타 선택지 응답 여부
 */
data class ResponseDetail(
    val content: String,
    val isOther: Boolean = false,
)
