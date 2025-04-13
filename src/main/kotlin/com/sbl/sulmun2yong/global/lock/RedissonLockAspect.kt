package com.sbl.sulmun2yong.global.lock

import com.sbl.sulmun2yong.global.lock.exception.InvalidLockKeyExpressionException
import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Aspect
@Component
class RedissonLockAspect(
    private val redissonClient: RedissonClient,
) {
    private val parser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(redissonLockAnnotation)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        redissonLockAnnotation: RedissonLock,
    ): Any? {
        // SpEL 평가를 위한 메서드 인자 정보 설정
        val methodSignature = joinPoint.signature as MethodSignature
        val parameterNames = nameDiscoverer.getParameterNames(methodSignature.method) ?: emptyArray()
        val args = joinPoint.args
        val context = StandardEvaluationContext()
        parameterNames.forEachIndexed { index, name ->
            context.setVariable(name, args[index])
        }

        // SpEL 표현식을 통해 락 키 생성
        val lockKey =
            parser
                .parseExpression(redissonLockAnnotation.key)
                .getValue(context, String::class.java)
                ?: throw InvalidLockKeyExpressionException()

        val lock = redissonClient.getLock(lockKey)

        // 지정한 waitTime 동안 락을 획득 시도 후, leaseTime 동안 유지
        val acquired = lock.tryLock(redissonLockAnnotation.waitTime, redissonLockAnnotation.leaseTime, TimeUnit.SECONDS)
        if (!acquired) throw TooManyLockRequestException()
        try {
            return joinPoint.proceed()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
