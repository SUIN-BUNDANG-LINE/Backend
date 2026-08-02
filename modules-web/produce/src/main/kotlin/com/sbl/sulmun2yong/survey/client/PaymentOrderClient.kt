package com.sbl.sulmun2yong.survey.client

import com.sbl.sulmun2yong.payment.dto.IssueOrderRequest
import com.sbl.sulmun2yong.payment.dto.IssueOrderResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.UUID

// 결제 서비스의 내부 주문 발급 API 클라이언트 - 설문 개시(단독 결제)가 동기 호출한다.
// payment_orders 쓰기는 결제만(단일 기록자) - checkoutUrl 동기 반환 계약 때문에 이벤트 대신 내부 API.
// 비밀 헤더(X-Gateway-Auth)를 부착해 결제의 GatewayOnlyFilter 를 통과한다(서비스간 신뢰).
@Component
class PaymentOrderClient(
    @Value("\${payment.base-url:http://localhost:8082}")
    paymentBaseUrl: String,
    @Value("\${gateway.secret:local-dev-secret}")
    gatewaySecret: String,
) {
    private val restTemplate =
        RestTemplate().apply {
            uriTemplateHandler = DefaultUriBuilderFactory(paymentBaseUrl)
            interceptors.add(
                ClientHttpRequestInterceptor { request, body, execution ->
                    request.headers.add("X-Gateway-Auth", gatewaySecret)
                    execution.execute(request, body)
                },
            )
        }

    fun issueOrder(
        surveyId: UUID,
        makerId: UUID,
        amount: Int,
    ): String =
        restTemplate
            .postForObject(
                "/internal/payments/orders",
                IssueOrderRequest(surveyId = surveyId, makerId = makerId, amount = amount),
                IssueOrderResponse::class.java,
            )!!
            .orderId
}
