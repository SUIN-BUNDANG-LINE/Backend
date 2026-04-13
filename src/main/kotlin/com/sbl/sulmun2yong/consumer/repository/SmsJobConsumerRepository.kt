package com.sbl.sulmun2yong.consumer.repository

import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsJobConsumerRepository : JpaRepository<SmsNotificationJobEntity, Long>
