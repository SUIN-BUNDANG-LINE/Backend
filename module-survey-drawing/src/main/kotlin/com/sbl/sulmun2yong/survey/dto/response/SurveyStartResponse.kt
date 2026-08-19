package com.sbl.sulmun2yong.survey.dto.response

// 설문 시작 요청의 결과 - 결제가 필요하면 설문은 아직 열리지 않았고, 개시는 모금 접수(1인 = 단독)로 이어간다
data class SurveyStartResponse(
    val paymentRequired: Boolean,
)
