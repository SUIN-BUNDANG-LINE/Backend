package com.sbl.sulmun2yong.notification.repository

import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DltMessageRepository : JpaRepository<DltMessageEntity, Long> {
    fun getNotificationType(): String

    fun getCount(): Long
}
