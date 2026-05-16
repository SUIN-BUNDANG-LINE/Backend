package com.sbl.sulmun2yong.notification.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "sms_notification_jobs",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_event_id_notification_type",
            columnNames = ["event_id", "notification_type"],
        ),
    ],
)
class SmsNotificationJobEntity(
    /** 고유 식별자 (Auto-increment) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    /** Kafka 이벤트 ID — 멱등성 키 (UNIQUE 제약의 일부) */
    @Column(name = "event_id", nullable = false, length = 36)
    val eventId: String,
    /** 알림 채널 구분 ("DRAWING_SMS" 등) — 같은 이벤트를 여러 채널로 발송할 때 구분 */
    @Column(name = "notification_type", nullable = false, length = 50)
    val notificationType: String,
    /** 원본 Kafka 메시지 JSON — Worker/Listener가 나중에 역직렬화하여 실제 발송에 사용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,
    /** 상태 머신: PENDING → EXECUTING → COMPLETED / FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SmsJobStatus = SmsJobStatus.PENDING,
    /** 재시도 횟수 — Exponential backoff 계산과 최대 재시도 판단에 사용 */
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    /** 다음 실행 가능 시점 — Exponential backoff로 계산 (2^(retryCount-1)초 후) */
    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Instant = Instant.now(),
    /** 마지막 실패 에러 메시지 — 디버깅/모니터링용 */
    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,
) : BaseTimeEntity() {
    companion object {
        fun create(
            eventId: String,
            notificationType: String,
            payload: String,
        ): SmsNotificationJobEntity =
            SmsNotificationJobEntity(
                eventId = eventId,
                notificationType = notificationType,
                payload = payload,
            )
    }

    fun markExecuting() {
        this.status = SmsJobStatus.EXECUTING
    }

    fun markCompleted() {
        this.status = SmsJobStatus.COMPLETED
        this.lastError = null
    }

    fun markFailedOrRetry(
        error: String,
        maxRetry: Int,
    ): Boolean {
        this.retryCount += 1
        if (retryCount > maxRetry) {
            this.status = SmsJobStatus.FAILED
            return true
        }
        this.status = SmsJobStatus.PENDING
        this.nextAttemptAt = Instant.now().plusSeconds(1L shl (retryCount - 1))
        return false
    }
}
