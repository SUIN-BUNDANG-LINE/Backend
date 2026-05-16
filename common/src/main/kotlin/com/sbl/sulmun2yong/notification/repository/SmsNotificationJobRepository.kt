package com.sbl.sulmun2yong.notification.repository

import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SmsNotificationJobRepository : JpaRepository<SmsNotificationJobEntity, Long> {
    fun countByStatus(status: SmsJobStatus): Long

    fun findByStatusAndNextAttemptAtLessThanEqual(
        status: SmsJobStatus,
        now: Instant,
    ): List<SmsNotificationJobEntity>
}
