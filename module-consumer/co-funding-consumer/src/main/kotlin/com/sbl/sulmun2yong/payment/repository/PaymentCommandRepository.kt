package com.sbl.sulmun2yong.payment.repository

import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PaymentCommandRepository : JpaRepository<PaymentCommand, UUID> {
    // CANCEL 이중 적재 사전 검사 - 최종 방어는 UNIQUE(aggregate_id, command_type)
    fun existsByAggregateIdAndCommandType(
        aggregateId: String,
        commandType: PaymentCommandType,
    ): Boolean
}
