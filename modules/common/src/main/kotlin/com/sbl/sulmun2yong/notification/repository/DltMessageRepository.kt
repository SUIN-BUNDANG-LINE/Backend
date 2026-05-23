package com.sbl.sulmun2yong.notification.repository

import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DltMessageRepository : JpaRepository<DltMessageEntity, Long> {
    @Query(
        """
        SELECT d.notificationType as notificationType, count(d) as count
        FROM DltMessageEntity  d
        group by d.notificationType
     """,
    )
    fun countGroupByNotificationType(): List<DltCountByType>
}
