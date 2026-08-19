package com.sbl.sulmun2yong.payment.controller

import com.sbl.sulmun2yong.payment.dto.PaymentFailRequest
import com.sbl.sulmun2yong.payment.dto.PaymentSuccessRequest
import com.sbl.sulmun2yong.payment.dto.response.ConfirmResultResponse
import com.sbl.sulmun2yong.payment.service.ConfirmOutcome
import com.sbl.sulmun2yong.payment.service.PaymentConfirmService
import com.sbl.sulmun2yong.payment.service.PaymentFailService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 결제 결과 처리 - success·fail 모두 정적 착지장(confirm.html)의 fetch 가 부른다.
// 부수효과(승인 확정·실패 기록)가 있어 POST - GET 의 안전 계약(프리페치·스캐너·자동 재시도) 밖이고,
// paymentKey·amount 가 쿼리스트링(접근 로그·히스토리)에 남지 않는다.
// JSON(state)만 돌려주고 화면 이동은 페이지 몫 - 착지 API 가 주소창에 남지 않아
// F5 안전이 구조로 성립한다(재호출돼도 confirm 도장 CAS·fail PENDING 가드가 멱등 흡수).
@RestController
@RequestMapping("/api/v1/payments")
class PaymentResultController(
    private val paymentConfirmService: PaymentConfirmService,
    private val paymentFailService: PaymentFailService,
) {
    @PostMapping("/success")
    fun success(
        @RequestBody request: PaymentSuccessRequest,
    ): ConfirmResultResponse {
        val outCome = paymentConfirmService.handleSuccess(request.paymentKey, request.orderId, request.amount)
        val state =
            when (outCome) {
                ConfirmOutcome.SUCCEEDED -> "succeeded"
                ConfirmOutcome.PROCESSING -> "processing"
                ConfirmOutcome.FAILED -> "failed"
            }

        return ConfirmResultResponse(state = state)
    }

    @PostMapping("/fail")
    fun fail(
        @RequestBody request: PaymentFailRequest,
    ): ConfirmResultResponse {
        paymentFailService.handleFail(request.orderId, request.code)
        return ConfirmResultResponse(state = "failed")
    }
}
