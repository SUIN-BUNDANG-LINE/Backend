package com.sbl.sulmun2yong.cofunding.experiment

import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.outbox.repository.KafkaRecordOutboxRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import java.lang.reflect.Proxy
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

// 파티션 병렬 소비 확장 실험(experiment 프로파일) 전용 배선.
// DB 자동설정(application-experiment.yml 에서 제외)을 대신해, 리스너가 주입받는
// 리포지토리를 "고정 지연 + 정해진 답"을 주는 프록시로 대체한다.
// 리스너(PaymentSucceededCoFundingListener)의 소비 경로는 한 줄도 바꾸지 않는다.
@Configuration
@Profile("experiment")
class ExperimentStubConfig(
    private val integrityRecorder: ExperimentIntegrityRecorder,
) {

    companion object {
        // DB 왕복 1회를 흉내내는 지연 - 컨슈머 인스턴스 1개의 처리 상한을 결정하는 실험 파라미터.
        // 핫패스가 3회(참여자 조회·모금 잠금 조회·장벽 CAS) 지불한다.
        private val DB_LATENCY_MS = System.getProperty("experiment.db-latency-ms", "2").toLong()

        private val FUNDING_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val SURVEY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        private val OWNER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    }

    // @Transactional 충족용 - DB 가 없으므로 커밋/롤백이 하는 일은 없다
    @Bean
    fun transactionManager(): PlatformTransactionManager =
        object : AbstractPlatformTransactionManager() {
            override fun doGetTransaction(): Any = Any()

            override fun doBegin(
                transaction: Any,
                definition: TransactionDefinition,
            ) {
            }

            override fun doCommit(status: DefaultTransactionStatus) {}

            override fun doRollback(status: DefaultTransactionStatus) {}
        }

    @Bean
    fun coFundingRepository(): CoFundingRepository =
        stub(
            mapOf(
                // 항상 "장벽 미통과 FUNDING 모금" - 매 메시지가 가장 흔한 경로(markPaid + CAS 실패)를 탄다
                "findByIdForUpdate" to { args ->
                    dbRoundTrip()
                    CoFunding(
                        id = args[0] as UUID,
                        surveyId = SURVEY_ID,
                        ownerId = OWNER_ID,
                        capacity = 1000,
                        shareAmount = 2000,
                        deadline = LocalDateTime.now().plusDays(1),
                        status = CoFundingStatus.FUNDING,
                    )
                },
                "tryConfirm" to { _ ->
                    dbRoundTrip()
                    0
                },
            ),
        )

    @Bean
    fun coFundingParticipantRepository(): CoFundingParticipantRepository =
        stub(
            mapOf(
                "findByTossOrderId" to { args ->
                    integrityRecorder.record(args[0] as String)
                    dbRoundTrip()
                    CoFundingParticipant(
                        id = UUID.randomUUID(),
                        fundingId = FUNDING_ID,
                        userId = UUID.randomUUID(),
                        tossOrderId = args[0] as String,
                    )
                },
            ),
        )

    // 아웃박스 릴레이 스케줄러·메트릭 폴링이 주입받는 리포지토리 - 항상 비어 있음(기본값 응답)
    @Bean
    fun kafkaRecordOutboxRepository(): KafkaRecordOutboxRepository = stub(emptyMap())

    private fun dbRoundTrip() = Thread.sleep(DB_LATENCY_MS)

    // 지정 메서드만 오버라이드하고 나머지는 반환 타입별 중립값을 주는 JDK 프록시 스텁
    private inline fun <reified T : Any> stub(overrides: Map<String, (Array<Any?>) -> Any?>): T {
        val iface = T::class.java
        val proxy =
            Proxy.newProxyInstance(iface.classLoader, arrayOf(iface)) { self, method, rawArgs ->
                val args = rawArgs ?: emptyArray()
                val override = overrides[method.name]
                when {
                    override != null -> override(args)
                    method.name == "equals" -> self === args.getOrNull(0)
                    method.name == "hashCode" -> System.identityHashCode(self)
                    method.name == "toString" -> "experiment-stub:${iface.simpleName}"
                    else -> neutralValue(method.returnType, args)
                }
            }
        return iface.cast(proxy)
    }

    private fun neutralValue(
        returnType: Class<*>,
        args: Array<Any?>,
    ): Any? =
        when (returnType) {
            List::class.java -> emptyList<Any>()
            Optional::class.java -> Optional.empty<Any>()
            Long::class.javaPrimitiveType, Long::class.javaObjectType -> 0L
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> 0
            Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> false
            else -> args.firstOrNull()?.takeIf { returnType.isInstance(it) } // save(entity) 류는 인자 반환
        }
}
