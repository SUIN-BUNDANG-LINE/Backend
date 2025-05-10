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
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.TimeUnit

@Aspect
@Component
class RedissonLockAspect(
    private val redissonClient: RedissonClient,
) : BeanFactoryAware {
    private lateinit var beanFactory: BeanFactory
    private val parser = SpelExpressionParser()
    private val paramNameDiscoverer = DefaultParameterNameDiscoverer()

    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    @Around(value = "@annotation(redissonLock)", argNames = "pjp,redissonLock")
    fun around(
        pjp: ProceedingJoinPoint,
        redissonLock: RedissonLock,
    ): Any? {
        // -------- 1. SpEL 로 Lock Key 계산 --------
        val method = (pjp.signature as MethodSignature).method
        val ctx =
            StandardEvaluationContext().apply {
                setBeanResolver { _, bn -> beanFactory.getBean(bn) }
                paramNameDiscoverer.getParameterNames(method)?.forEachIndexed { i, n ->
                    setVariable(n, pjp.args[i])
                }
            }
        val lockKey =
            parser
                .parseExpression(redissonLock.key)
                .getValue(ctx, String::class.java)
                ?: throw InvalidLockKeyExpressionException()

        // -------- 2. 분산락 획득 --------
        val lock = redissonClient.getLock(lockKey)
        val acquired =
            lock.tryLock(
                redissonLock.waitTime,
                redissonLock.leaseTime,
                TimeUnit.SECONDS,
            )
        if (!acquired) throw TooManyLockRequestException()

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
