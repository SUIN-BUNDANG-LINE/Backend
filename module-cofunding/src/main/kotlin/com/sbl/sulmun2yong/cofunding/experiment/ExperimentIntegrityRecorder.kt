package com.sbl.sulmun2yong.cofunding.experiment

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicLong

// 종단 무결성 기록기 - 페이로드 정수 N 을 DB(value 컬럼)에 영속화한다.
// UNIQUE(value) 가 멱등 백스톱: 재전달(at-least-once)은 received_count 만 +1 되고 합은 불변.
// JVM 이 죽어도(docker kill) 기록이 남아 "발행한 정수 전부, 각 1회" 판정이 성립한다.
// experiment.db-url 미지정 시 전부 무시(no-op) - 증설(run)·스모크 모드는 DB 없이 돈다.
@Component
@Profile("experiment")
class ExperimentIntegrityRecorder(
    registry: MeterRegistry,
) {
    private val url = System.getProperty("experiment.db-url", "")
    private val user = System.getProperty("experiment.db-user", "user")
    private val password = System.getProperty("experiment.db-password", "password")

    private val payloadSum = AtomicLong(0)
    private val uniqueCount = AtomicLong(0)
    private val duplicateCount = AtomicLong(0)

    private var conn: Connection? = null

    init {
        Gauge.builder("experiment_payload_sum", payloadSum) { it.get().toDouble() }.register(registry)
        Gauge.builder("experiment_unique_count", uniqueCount) { it.get().toDouble() }.register(registry)
        Gauge.builder("experiment_duplicate_count", duplicateCount) { it.get().toDouble() }.register(registry)
    }

    @Synchronized
    private fun connection(): Connection? {
        if (url.isBlank()) return null
        val existing = conn
        if (existing != null && !existing.isClosed) return existing
        return runCatching {
            DriverManager.getConnection(url, user, password).also {
                it.createStatement().use { st ->
                    st.execute(
                        "CREATE TABLE IF NOT EXISTS experiment_received (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                            "`value` BIGINT NOT NULL, " +
                            "received_count INT NOT NULL DEFAULT 1, " +
                            "UNIQUE KEY uk_experiment_received_value (`value`))",
                    )
                }
                conn = it
            }
        }.getOrNull()
    }

    @Synchronized
    fun record(orderId: String) {
        val n = orderId.substringAfterLast('-').toLongOrNull() ?: return
        val c = connection() ?: return
        runCatching {
            c
                .prepareStatement(
                    "INSERT INTO experiment_received (`value`) VALUES (?) " +
                        "ON DUPLICATE KEY UPDATE received_count = received_count + 1",
                ).use { ps ->
                    ps.setLong(1, n)
                    ps.executeUpdate()
                }
        }.onFailure { conn = null }
    }

    @Scheduled(fixedDelay = 5000)
    fun refreshGauges() {
        val c = connection() ?: return
        runCatching {
            c.createStatement().use { st ->
                st
                    .executeQuery(
                        "SELECT COUNT(*), COALESCE(SUM(`value`), 0), COALESCE(SUM(received_count - 1), 0) " +
                            "FROM experiment_received",
                    ).use { rs ->
                        if (rs.next()) {
                            uniqueCount.set(rs.getLong(1))
                            payloadSum.set(rs.getLong(2))
                            duplicateCount.set(rs.getLong(3))
                        }
                    }
            }
        }.onFailure { conn = null }
    }
}
