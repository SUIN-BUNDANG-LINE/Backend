package com.sbl.sulmun2yong.notification.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "dlt_messages")
class DltMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "event_id", nullable = false, length = 36)
    val eventId: String,
    @Column(name = "notification_type", nullable = false, length = 50)
    val notificationType: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "retry_count", nullable = false)
    val retryCount: Int,
    @Column(name = "last_error", columnDefinition = "TEXT")
    val lastError: String?,
    @Column(name = "failed_at", nullable = false)
    val failedAt: Instant,
    @Column(nullable = false)
    var reprocessed: Boolean = false,
) : BaseTimeEntity()
