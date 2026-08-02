package com.sbl.sulmun2yong.payment.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.dto.TossConfirmRequest
import com.sbl.sulmun2yong.payment.dto.TossErrorResponse
import com.sbl.sulmun2yong.payment.dto.TossPaymentResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

// 토스페이먼츠 코어 API 어댑터 - Basic Auth & base-url은 tossPaymentsTemplate 빈이 담당.
@Component
class TossPaymentsAdapter(
    private val tossPaymentsTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
) {
    // 결제 승인 - Idempotency-Key=orderId 로 재시도해도 1회만 승인된다.
    fun confirm(
        paymentKey: String,
        orderId: String,
        amount: Int,
    ): TossConfirmResult {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Idempotency-key", orderId)
            }

        val request = HttpEntity(TossConfirmRequest(paymentKey, orderId, amount), headers)

        return try {
            val payment =
                tossPaymentsTemplate.postForObject(
                    "/v1/payments/confirm",
                    request,
                    TossPaymentResponse::class.java,
                ) ?: return TossConfirmResult.Unknown

            if (payment.status == "DONE") {
                TossConfirmResult.Approved(payment.paymentKey)
            } else {
                TossConfirmResult.Rejected(payment.status, "승인 응답이지만 DONE 아님")
            }
        } catch (e: HttpClientErrorException) {
            // 4xx - 원칙적으론 명시적 거절. 닩 "이미 처리됨"은 과거 재시도가 성공한 흔적이라
            // 조회("그 결제 어떻게 됐어?" = getOrder)로 확정한다.
            val code = parseErrorCode(e)
            if (code == "ALREADY_PROCESSED_PAYMENT") {
                log.info(
                    "confirm 멱등 재시도 - 이미 처리됨, 조회로 수렴 필요: orderId={}",
                    orderId,
                )
                TossConfirmResult.Unknown
            } else {
                log.warn("confirm 거절: orderId={}, code={}", orderId, code)
                TossConfirmResult.Rejected(code, e.responseBodyAsString.take(200))
            }
        } catch (e: RestClientException) {
            // 타임아웃 & 5xx & 네트워크 - 승인 여부를 모른다. 실패가 아니다!
            log.warn("confirm 미확정(재시도 대상): orderId={}", orderId, e)
            TossConfirmResult.Unknown
        }
    }

    // 전액 취소 - Idempotency-Key="cancel:{orderId}" 로 재시도해도 1회만 취소된다.
    // 반환은 confirm 과 같은 삼분법을 공유한다: Approved = 취소 확정(카드사 취소 승인),
    // Rejected = 명시적 거절(재시도 무의미), Unknown = 미확정(재시도 & 조회 수렴 대상).
    // 이미 취소됨(ALREADY_CANCEL_PAYMENT)은 과거 시도가 성공한 흔적이라 Approved 로 간주한다
    fun cancel(
        paymentKey: String,
        orderId: String,
        cancelReason: String,
    ): TossConfirmResult {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Idempotency-key", "cancel:$orderId")
            }

        val request = HttpEntity(TossCancelRequest(cancelReason), headers)

        return try {
            val payment =
                tossPaymentsTemplate.postForObject(
                    "/v1/payments/{paymentKey}/cancel",
                    request,
                    TossPaymentResponse::class.java,
                    paymentKey,
                ) ?: return TossConfirmResult.Unknown

            if (payment.status == "CANCELED") {
                TossConfirmResult.Approved(payment.paymentKey)
            } else {
                TossConfirmResult.Rejected(payment.status, "취소 요청을 보냈지만 CANCELED 응답을 받지 않았습니다")
            }
        } catch (e: HttpClientErrorException) {
            val code = parseErrorCode(e)
            if (code == "ALREADY_CANCELED_PAYMENT") {
                log.info("cancel이 재시도되었습니다. - 이미 취소되었습니다: orderId={}", orderId)
                TossConfirmResult.Approved(paymentKey)
            } else {
                log.warn("cancel 거절: orderId={}, code={}", orderId, code)
                TossConfirmResult.Rejected(code, e.responseBodyAsString.take(200))
            }
        } catch (e: RestClientException) {
            log.warn("cancel 미확정(재시도 대상): orderId={}", orderId, e)
            TossConfirmResult.Unknown
        }
    }

    fun getOrder(orderId: String): TossPaymentResponse? =
        try {
            tossPaymentsTemplate.getForObject(
                "/v1/payments/orders/{orderId}",
                TossPaymentResponse::class.java,
                orderId,
            )
        } catch (e: RestClientException) {
            log.warn("주문 조회 실패: orderId={}", orderId)
            null
        }

    private fun parseErrorCode(e: HttpClientErrorException): String =
        runCatching {
            objectMapper
                .readValue(
                    e.responseBodyAsString,
                    TossErrorResponse::class.java,
                ).code
        }.getOrDefault("UNKNOWN")

    companion object {
        private val log = LoggerFactory.getLogger(TossPaymentsAdapter::class.java)
    }

}
