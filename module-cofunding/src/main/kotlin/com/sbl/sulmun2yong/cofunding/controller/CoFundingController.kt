package com.sbl.sulmun2yong.cofunding.controller

import com.sbl.sulmun2yong.cofunding.dto.request.CoFundingStartRequest
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingMyOrderResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStartResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStatusResponse
import com.sbl.sulmun2yong.cofunding.service.CoFundingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

// 모금 서비스의 동기 API - 게이트웨이가 검증·주입한 X-User-Id 헤더만 신뢰한다(PURE).
// @LoginUser 리졸버 대신 헤더 직접 바인딩 - 이 모듈은 스프링 시큐리티 없이 GatewayOnlyFilter 로 지킨다.
@RestController
@RequestMapping("/api/v1")
class CoFundingController(
    private val coFundingService: CoFundingService,
) {
    @PostMapping("/surveys/{surveyId}/co-funding")
    fun start(
        @PathVariable surveyId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CoFundingStartRequest,
    ): ResponseEntity<CoFundingStartResponse> = ResponseEntity.ok(coFundingService.start(surveyId, userId, request))

    @GetMapping("/co-fundings/{fundingId}/participants/me/order")
    fun myOrder(
        @PathVariable fundingId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<CoFundingMyOrderResponse> = ResponseEntity.ok(coFundingService.findMyOrder(fundingId, userId))

    // 접수(PENDING_APPROVAL) 후 판정 확정(FUNDING/REJECTED)을 폴링하는 상태 조회
    @GetMapping("/co-fundings/{fundingId}")
    fun status(
        @PathVariable fundingId: UUID,
    ): ResponseEntity<CoFundingStatusResponse> = ResponseEntity.ok(coFundingService.findStatus(fundingId))
}
