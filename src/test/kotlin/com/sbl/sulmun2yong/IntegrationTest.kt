package com.sbl.sulmun2yong

import org.springframework.boot.test.context.SpringBootTest

// 통합 테스트용 메타 애너테이션 — web/consumer 양쪽 빈을 모두 로딩.
// 신규 통합 테스트는 @SpringBootTest 대신 이 애너테이션을 사용한다.
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(classes = [Sulmun2yongApplication::class, Sulmun2yongConsumerApplication::class])
annotation class IntegrationTest
