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
                    "confirm 200 응답이지만 DONE 아님(조회로 수렴) - orderId={}, tossStatus={}, totalAmount={}, paymentKey={}",
                    orderId,
                    payment.status,
                    payment.totalAmount,
                    payment.paymentKey,
                )
                TossConfirmResult.Unknown
            }
        } catch (e: HttpClientErrorException) {
            val code = parseErrorCode(e)
            val body = e.responseBodyAsString.take(200)
            when (code) {
                // 한도초과·잔액부족은 REJECT_CARD_PAYMENT 로 온다 - 둘을 code 로 사후 구분한다
                "REJECT_CARD_COMPANY", "REJECT_CARD_PAYMENT" -> {
                    log.warn(
                        "confirm 카드사 거절 - 한도초과·잔액부족·정지 등: orderId={}, code={}, amount={}, body={}",
                        orderId,
                        code,
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

                // 금액 위변조 등 요청 자체가 거부됨 - 같은 본문으로는 재시도해도 동일하다
                "FORBIDDEN_REQUEST" -> {
                    log.warn(
                        "confirm 요청 거부 - 금액 위변조 의심: orderId={}, amount={}, body={}",
                        orderId,
                        amount,
                        body,
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                "UNAUTHORIZED_KEY", "INVALID_API_KEY" -> {
                    // 설정 사고라 키를 고치면 성공한다 - 장부를 FAILED 로 확정하지 않는다
                    log.error(
                        "confirm 시크릿 키 인증 실패 - 설정 사고, 수동 확인 필요: orderId={}, httpStatus={}",
                        orderId,
                        e.statusCode.value(),
                    )
                    TossConfirmResult.Unknown
                }

                "ALREADY_PROCESSED_PAYMENT" -> {
                    log.info(
                        "confirm 멱등 재시도 - 이미 처리됨, 조회로 수렴 필요: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Unknown
                }

                "IDEMPOTENT_REQUEST_PROCESSING" -> {
                    log.info(
                        "confirm 선행 요청 처리 중 - 승인 여부를 모른다, 릴레이 재시도 대상: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Unknown
                }

                // 미확인 코드 - PROVIDER_ERROR 등 일시적 오류와 신규 코드가 섞인다.
                // 종착 여부는 어댑터가 코드명으로 추측하지 않고 resolveUnknown 의 조회가 판정한다.
                else -> {
                    log.warn(
                        "confirm 미확인 코드(조회로 수렴) - orderId={}, code={}, httpStatus={}, amount={}, paymentKey={}, body={}",
                        orderId,
                        code,
                        e.statusCode.value(),
                        amount,
                        paymentKey,
                        body,
                    )
                    TossConfirmResult.Unknown
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
        retryCount: Int,
    ): TossConfirmResult {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                // 회차를 섞는다 - orderId 만으로는 고정된 실패 응답에서 벗어날 수 없다
                set("Idempotency-key", "cancel:$orderId:$retryCount")
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
                // 종착 여부를 여기서 확정하지 않는다 - resolveCancelUnknown 의 조회가 판정한다.
                // 주의: 부분취소로 잔액을 전부 소진해도 상태는 PARTIAL_CANCELED 라 조회가 완료로 보지 않는다.
                log.warn(
                    "cancel 2xx 응답이지만 CANCELED 아님(조회로 수렴) - orderId={}, tossStatus={}, paymentKey={}",
                    orderId,
                    payment.status,
                    payment.paymentKey,
                )
                TossConfirmResult.Unknown
            }
        } catch (e: HttpClientErrorException) {
            val code = parseErrorCode(e)
            val body = e.responseBodyAsString.take(200)
            when (code) {
                // 이미 종착한 건 - 중복 취소의 실질 방어선이라 성공으로 흡수한다
                "ALREADY_CANCELED_PAYMENT", "ALREADY_REFUND_PAYMENT" -> {
                    log.info(
                        "cancel 멱등 재시도 - 이미 취소·환불됨, 성공으로 흡수: orderId={}, code={}, paymentKey={}",
                        orderId,
                        code,
                        paymentKey,
                    )
                    TossConfirmResult.Approved(paymentKey)
                }

                // 기간 만료·취소 불가 - 시간이 갈수록 확실해지는 종착이라 재시도하지 않는다
                "EXCEED_MAX_REFUND_DUE", "NOT_CANCELABLE_PAYMENT" -> {
                    log.error(
                        "cancel 불가 확정 - 수동 확인 필요: orderId={}, code={}, paymentKey={}, body={}",
                        orderId,
                        code,
                        paymentKey,
                        body,
                    )
                    TossConfirmResult.Rejected(code, body)
                }

                "IDEMPOTENT_REQUEST_PROCESSING" -> {
                    log.info(
                        "cancel 선행 요청 처리 중 - 취소 여부를 모른다, 릴레이 재시도 대상: orderId={}, paymentKey={}",
                        orderId,
                        paymentKey,
                    )
                    TossConfirmResult.Unknown
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
                    // 설정 사고라 키를 고치면 성공한다 - 커맨드를 FAILED 로 확정하지 않는다
                    log.error(
                        "cancel 시크릿 키 인증 실패 - 설정 사고, 수동 확인 필요: orderId={}, httpStatus={}",
                        orderId,
                        e.statusCode.value(),
                    )
                    TossConfirmResult.Unknown
                }

                // 미확인 코드 - PROVIDER_ERROR("잠시 후 다시 시도") 등 일시적 오류가 섞인다.
                // 종착 여부는 resolveCancelUnknown 의 조회가 판정한다.
                else -> {
                    log.warn(
                        "cancel 미확인 코드(조회로 수렴) - orderId={}, code={}, httpStatus={}, paymentKey={}, body={}",
                        orderId,
                        code,
                        e.statusCode.value(),
                        paymentKey,
                        body,
                    )
                    TossConfirmResult.Unknown
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
