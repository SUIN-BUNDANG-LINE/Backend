package com.sbl.sulmun2yong.global.lock

import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import com.sbl.sulmun2yong.global.lock.metrics.DrawingLockMetrics
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.TimeUnit

@Aspect
@Component
class RedissonLockAspect(
    private val redissonClient: RedissonClient,
    private val metrics: DrawingLockMetrics,
) {
    private val paramNameDiscoverer = DefaultParameterNameDiscoverer()

    // @RedissonLock이 붙은 메서드 호출을 가로채서 락 획득/해제를 감싼다
    @Around(value = "@annotation(redissonLock)", argNames = "pjp,redissonLock")
    fun around(
        // pjp: 가로챈 원본 메서드의 정보 (시그니처, 인자값, proceed()로 실행)
        pjp: ProceedingJoinPoint,
        // redissonLock: 메서드에 선언된 @RedissonLock 어노테이션의 속성값 (key, leaseTime, waitTime)
        redissonLock: RedissonLock,
    ): Any? {
        // -------- 1. Lock Key 계산: key 문자열의 {파라미터명}을 실제 값으로 치환 --------
        // 예) processDrawing(surveyId=UUID("abc-123"), participantId=UUID("def-456"), ...)
        //     key = "drawingLock:{surveyId}" → "drawingLock:abc-123"

        // AOP가 가로챈 메서드의 Method 객체 추출
        val method = (pjp.signature as MethodSignature).method
        // 메서드의 파라미터 이름 목록 → ["surveyId", "participantId", "selectedNumber", "phoneNumber"]
        val paramNames = paramNameDiscoverer.getParameterNames(method)
        // @RedissonLock(key = "drawingLock:{surveyId}") 에서 key 속성 값을 가져옴
        // → redissonLock.key = "drawingLock:{surveyId}"
        var lockKey = redissonLock.key
        // 파라미터 이름과 실제 인자값(pjp.args)을 인덱스로 1:1 매칭하여 치환
        // i=0: "{surveyId}" → args[0].toString() = "abc-123"
        // i=1~3: "{participantId}" 등은 key에 없으므로 변화 없음
        paramNames?.forEachIndexed { i, name ->
            lockKey = lockKey.replace("{$name}", pjp.args[i].toString())
        }

        // -------- 2. 분산락 획득 --------
        val lockKeyType = redissonLock.key
        val sample = metrics.startSample()
        // lockKey(예: "drawingLock:abc-123")에 해당하는 Redis 분산 락 객체 조회
        val lock = redissonClient.getLock(lockKey)
        // waitTime(5초) 동안 락 획득 시도, 성공 시 leaseTime(10초) 후 자동 해제
        // true: 락 획득 성공 / false: 대기 시간 내 획득 실패 (다른 스레드가 점유 중)
        val acquired =
            lock.tryLock(
                redissonLock.waitTime,
                redissonLock.leaseTime,
                TimeUnit.SECONDS,
            )
        // 락 획득 실패 시 429 응답으로 요청 거부
        if (!acquired) {
            metrics.recordAcquire("failure", lockKeyType, sample)
            throw TooManyLockRequestException()
        }
        metrics.recordAcquire("success", lockKeyType, sample)

        try {
            // -------- 3. 비즈니스 로직 실행 (기존 트랜잭션에 참여) --------
            return pjp.proceed()
        } finally {
            // -------- 4. 트랜잭션 유무에 따라 unlock 시점 결정 --------
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                // 트랜잭션이 있으면 커밋/롤백 완료 후 해제
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCompletion(status: Int) {
                            if (lock.isHeldByCurrentThread) lock.unlock()
                        }
                    },
                )
            } else {
                // 트랜잭션이 없으면 즉시 해제
                if (lock.isHeldByCurrentThread) lock.unlock()
            }
        }
    }
}
