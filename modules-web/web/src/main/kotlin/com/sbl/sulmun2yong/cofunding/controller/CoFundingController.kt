package com.sbl.sulmun2yong.cofunding.controller

import com.sbl.sulmun2yong.cofunding.dto.request.CoFundingStartRequest
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingMyOrderResponse
import com.sbl.sulmun2yong.cofunding.dto.response.CoFundingStartResponse
import com.sbl.sulmun2yong.cofunding.service.CoFundingService
import com.sbl.sulmun2yong.global.annotation.LoginUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1")
class CoFundingController(
    private val coFundingService: CoFundingService,
) {
    @PostMapping("/surveys/{surveyId}/co-funding")
    fun start(
        @PathVariable surveyId: UUID,
        @LoginUser id: UUID,
        @RequestBody request: CoFundingStartRequest,
    ): ResponseEntity<CoFundingStartResponse> = ResponseEntity.ok(coFundingService.start(surveyId, id, request))

    @GetMapping("/co-fundings/{fundingId}/participants/me/order")
    fun myOrder(
        @PathVariable fundingId: UUID,
        @LoginUser id: UUID,
    ): ResponseEntity<CoFundingMyOrderResponse> = ResponseEntity.ok(coFundingService.findMyOrder(fundingId, id))
}
