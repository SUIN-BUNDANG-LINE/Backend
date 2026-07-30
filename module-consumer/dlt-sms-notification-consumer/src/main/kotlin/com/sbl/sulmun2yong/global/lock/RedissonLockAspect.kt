package com.sbl.sulmun2yong.global.lock

import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import com.sbl.sulmun2yong.global.lock.metrics.DrawingLockMetrics
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
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
        pjp: ProceedingJoinPoint,
        redissonLock: RedissonLock,
    ): Any? {
        val lockKey = resolveLockKey(pjp, redissonLock.key)
        val lock = acquireLockOrThrow(lockKey, redissonLock)
        try {
            return pjp.proceed()
        } finally {
            registerUnlockHook(lock)
        }
    }

    // key 템플릿의 {파라미터명} 을 실제 인자값으로 치환.
    // 예) "drawingLock:{surveyId}" + args=[UUID("abc")] → "drawingLock:abc"
    private fun resolveLockKey(
        pjp: ProceedingJoinPoint,
        keyTemplate: String,
    ): String {
        val method = (pjp.signature as MethodSignature).method
        val paramNames = paramNameDiscoverer.getParameterNames(method)
        var resolved = keyTemplate
        paramNames?.forEachIndexed { i, name ->
            resolved = resolved.replace("{$name}", pjp.args[i].toString())
        }
        return resolved
    }

    // waitTime 동안 락 획득 시도. 성공 시 metric 기록 후 lock 반환, 실패 시 metric + 429 예외.
    // lockKeyType 은 원본 template (치환 전) 으로 태깅 — 동일 패턴의 키들이 한 시리즈로 집계되도록.
    private fun acquireLockOrThrow(
        lockKey: String,
        redissonLock: RedissonLock,
    ): RLock {
        val lockKeyType = redissonLock.key
        val sample = metrics.startSample()
        val lock = redissonClient.getLock(lockKey)
        val acquired = lock.tryLock(redissonLock.waitTime, redissonLock.leaseTime, TimeUnit.SECONDS)
        if (!acquired) {
            metrics.recordAcquire("failure", lockKeyType, sample)
            throw TooManyLockRequestException()
        }
        metrics.recordAcquire("success", lockKeyType, sample)
        return lock
    }

    // 트랜잭션이 살아있으면 커밋/롤백 이후에 unlock, 없으면 즉시 unlock.
    private fun registerUnlockHook(lock: RLock) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCompletion(status: Int) {
                        if (lock.isHeldByCurrentThread) lock.unlock()
                    }
                },
            )
        } else {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }
}
