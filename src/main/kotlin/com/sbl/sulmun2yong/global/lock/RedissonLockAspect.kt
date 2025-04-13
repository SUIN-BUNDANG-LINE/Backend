package com.sbl.sulmun2yong.global.lock

import com.sbl.sulmun2yong.global.lock.exception.InvalidLockKeyExpressionException
import com.sbl.sulmun2yong.global.lock.exception.TooManyLockRequestException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Aspect
@Component
class RedissonLockAspect(
    private val redissonClient: RedissonClient,
) : BeanFactoryAware {
    private lateinit var beanFactory: BeanFactory
    private val parser = SpelExpressionParser()
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    @Around("@annotation(redissonLock)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        redissonLock: RedissonLock,
    ): Any? {
        // SpEL 평가 컨텍스트 생성 및 BeanResolver 등록
        val context = StandardEvaluationContext()
        context.setBeanResolver { _: org.springframework.expression.EvaluationContext, beanName: String ->
            beanFactory.getBean(beanName)
        }

        // 메서드 인자 이름 및 값 설정
        val methodSignature = joinPoint.signature as MethodSignature
        val parameterNames = parameterNameDiscoverer.getParameterNames(methodSignature.method) ?: emptyArray()
        parameterNames.forEachIndexed { i, name ->
            context.setVariable(name, joinPoint.args[i])
        }

        // SpEL 식을 평가하여 동적 락 키 생성
        val lockKey =
            parser
                .parseExpression(redissonLock.key)
                .getValue(context, String::class.java)
                ?: throw InvalidLockKeyExpressionException()

        // Redisson 락 획득
        val lock = redissonClient.getLock(lockKey)
        val acquired = lock.tryLock(redissonLock.waitTime, redissonLock.leaseTime, TimeUnit.SECONDS)
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
