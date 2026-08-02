package com.sbl.sulmun2yong.survey.domain.routing

enum class RoutingType {
    /** 번호순 라우팅 */
    NUMERICAL_ORDER,

    /** 선택지 기반 라우팅 */
    SET_BY_CHOICE,

    /** 사용자 설정 라우팅 */
    SET_BY_USER,
}
