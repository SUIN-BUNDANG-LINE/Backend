package com.sbl.sulmun2yong.drawing.service

// import org.junit.jupiter.api.Assertions.assertEquals
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.boot.test.context.SpringBootTest
// import java.util.UUID
// import java.util.concurrent.CountDownLatch
// import java.util.concurrent.Executors
// import java.util.concurrent.TimeUnit
//
// @SpringBootTest
class DrawingBoardServiceConcurrencyTest(
    // @Autowired val drawingService: DrawingBoardService,
)
// {
//     @Test
//     fun `동시 요청 테스트 - 단 하나의 추첨만 성공`() {
//         // 추첨 번호
//         val selectedNumber = 5
//
//         // 실제 참가자 정보로 대체해야함
//         val participantInfos =
//             listOf(
//                 Pair(UUID.fromString(""), "01000000000"),
//                 Pair(UUID.fromString(""), "01000000001"),
//                 Pair(UUID.fromString(""), "01000000002"),
//                 Pair(UUID.fromString(""), "01000000003"),
//                 Pair(UUID.fromString(""), "01000000004"),
//             )
//
//         val threadCount = participantInfos.size
//         val executor = Executors.newFixedThreadPool(threadCount)
//         val startLatch = CountDownLatch(1)
//         val results = mutableListOf<String>()
//
//         // 각 참가자 정보를 기준으로 동시에 요청을 수행합니다.
//         participantInfos.forEach { (participantId, phoneNumber) ->
//             executor.submit {
//                 startLatch.await()
//                 try {
//                     drawingService.doDrawing(participantId, selectedNumber, phoneNumber)
//                     synchronized(results) {
//                         results.add("success: ($participantId, $phoneNumber)")
//                     }
//                 } catch (e: Exception) {
//                     synchronized(results) {
//                         results.add("failure: ($participantId, $phoneNumber): ${e.message}")
//                     }
//                 }
//             }
//         }
//
//         startLatch.countDown()
//         executor.shutdown()
//         executor.awaitTermination(10, TimeUnit.SECONDS)
//
//         // 중복 참여 체크 로직에 의해 오직 하나의 요청만 정상 처리되어야 합니다.
//         val successCount = results.count { it.startsWith("success") }
//         assertEquals(1, successCount, "동시에 한 요청만 성공해야 합니다. 결과: $results")
//     }
// }
