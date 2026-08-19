package com.sbl.sulmun2yong.payment.repository

import com.sbl.sulmun2yong.payment.entity.TossOrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

// PK = 토스 orderId(String) - successUrl·웹훅·릴레이의 지배적 조회가 전부 findById 다.
interface TossOrderRepository : JpaRepository<TossOrderEntity, String> {

}
