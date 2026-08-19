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
    fun confirm(
        paymentKey: String,
        orderId: String,
        amount: Int,
    ): TossConfirmResult {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Idempotency-key", paymentKey)
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
                log.warn(
                    "confirm 200 응답이지만 미승인 상태 - orderId={}, tossStatus={}, totalAmount={}, paymentKey={}",
                    orderId,
                    payment.status,
                    payment.totalAmount,
                    payment.paymentKey,
                )
                TossConfirmResult.Rejected(payment.status, "승인 응답이지만 DONE 아님")
            }
        } catch (e: HttpClientErrorException) {
            val code = parseErrorCode(e)
            val body = e.responseBodyAsString.take(200)
            when (code) {
                "ALREADY_PROCESSED_PAYMENT" -> {
                    log.info(
                        "confirm 멱등 재시도 - 이미 처리됨, 조회로 수렴 필요: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Unknown
                }

                "REJECT_CARD_COMPANY" -> {
                    log.warn(
                        "confirm 카드사 거절 - 한도·정지 등 카드 사유: orderId={}, amount={}, body={}",
                        orderId,
                        amount,
                        body,
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                "NOT_FOUND_PAYMENT_SESSION" -> {
                    log.warn(
                        "confirm 가승인 세션 만료·소멸 - 승인 후 방치된 건: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                "UNAUTHORIZED_KEY", "INVALID_API_KEY" -> {
                    log.error(
                        "confirm 시크릿 키 인증 실패 - 설정 사고, 수동 확인 필요: orderId={}, httpStatus={}",
                        orderId,
                        e.statusCode.value(),
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                else -> {
                    log.warn(
                        "confirm 거절 - orderId={}, code={}, httpStatus={}, amount={}, paymentKey={}, body={}",
                        orderId,
                        code,
                        e.statusCode.value(),
                        amount,
                        paymentKey,
                        body,
                    )
                    TossConfirmResult.Rejected(code, body)
                }
            }
        } catch (e: RestClientException) {
            // 타임아웃 & 5xx & 네트워크 - 승인 여부를 모른다. 실패가 아니다!
            log.warn("confirm 미확정(재시도 대상): orderId={}", orderId, e)
            TossConfirmResult.Unknown
        }
    }

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
            val body = e.responseBodyAsString.take(200)
            when (code) {
                "ALREADY_CANCELED_PAYMENT" -> {
                    log.info(
                        "cancel 멱등 재시도 - 이미 취소됨, 성공으로 흡수: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Approved(paymentKey)
                }

                "NOT_FOUND_PAYMENT" -> {
                    log.error(
                        "cancel 대상 결제 없음 - 데이터 이상, 수동 확인 필요: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                "UNAUTHORIZED_KEY", "INVALID_API_KEY" -> {
                    log.error(
                        "cancel 시크릿 키 인증 실패 - 설정 사고, 수동 확인 필요: orderId={}, httpStatus={}",
                        orderId,
                        e.statusCode.value(),
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                else -> {
                    log.warn(
                        "cancel 거절 - orderId={}, code={}, httpStatus={}, paymentKey={}, body={}",
                        orderId,
                        code,
                        e.statusCode.value(),
                        paymentKey,
                        body,
                    )
                    TossConfirmResult.Rejected(code, body)
                }
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
