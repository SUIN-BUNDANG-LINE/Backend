package com.sbl.sulmun2yong.global.lock

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedissonLock(
    /**
     * 락 키 템플릿. {파라미터명}을 메서드 인자값으로 치환
     * 예: "drawingLock:{surveyId}"
     */
    val key: String,
    /** 락이 자동 해제되는 시간(초) */
    val leaseTime: Long = 10,
    /**
     * 락 획득 시 대기 시간(초). 0이면 즉시 실패합니다.
     */
    val waitTime: Long = 0,
)
