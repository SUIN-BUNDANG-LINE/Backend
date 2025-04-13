package com.sbl.sulmun2yong.global.lock

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedissonLock(
    /**
     * SpEL 표현식을 이용하여 동적 락 키 생성
     * 예: "'drawingLock:' + #participantId + ':' + #selectedNumber"
     */
    val key: String,
    /** 락이 자동 해제되는 시간(초) */
    val leaseTime: Long = 10,
    /**
     * 락 획득 시 대기 시간(초). 0이면 즉시 실패합니다.
     */
    val waitTime: Long = 0,
)
