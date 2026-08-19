package com.sbl.sulmun2yong.global.kafka

// Outbox 로 발행되는 이벤트의 공통 계약.
// eventId 는 발행 1회의 지문이자 아웃박스 행의 PK - 컨슈머가 남긴 eventId 로
// 프로듀서 쪽 발행 이력을 PK 조회 한 번에 되짚을 수 있다.
interface DomainEvent {
    val eventId: String
}
