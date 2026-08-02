package com.sbl.sulmun2yong.payment.entity

// PG로 보내는 명령 종류 — MVP는 AUTHORIZE. 매입·부분취소는 후속(consumer 레포 이식).
enum class PaymentCommandType {
    CONFIRM,
    CANCEL,
}
