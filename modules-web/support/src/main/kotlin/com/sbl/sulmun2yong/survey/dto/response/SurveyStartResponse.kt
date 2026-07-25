package com.sbl.sulmun2yong.survey.dto.response

// 설문 시작 요청의 결과 - 결제가 필요하면 설문은 아직 열리지 않았고 checkoutUrl로 가야한다
data class SurveyStartResponse(
    val paymentRequired: Boolean,
    val checkoutUrl: String?,
)
