package com.sbl.sulmun2yong.payment.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.*

// 토스 API 통신용 RestTemplate - 결제 서비스가 어댑터(TossPaymentsAdapter)의 유일한 구동처다.
// (web 의 RestTemplateConfig 에서 toss 빈을 가져옴 - Phase 4 이관)
@Configuration
class TossRestTemplateConfig(
    @Value("\${toss.base-url}")
    private val tossBaseUrl: String,
    @Value("\${toss.secret-key}")
    private val tossSecretKey: String,
) {
    @Bean
    fun tossPaymentsTemplate(): RestTemplate {
        val objectMapper =
            ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        // Basic Auth: base64(secretKey + ":" ) - 시크릿 키가 아이디, 비밀번호는 빈 값
        val encodedKey = Base64.getEncoder().encodeToString("$tossSecretKey:".toByteArray())

        return RestTemplate(listOf(MappingJackson2HttpMessageConverter(objectMapper)))
            .apply {
                uriTemplateHandler = DefaultUriBuilderFactory(tossBaseUrl)
                interceptors.add(
                    ClientHttpRequestInterceptor { request, body, execution ->
                        request.headers.add("Authorization", "Basic $encodedKey")
                        execution.execute(request, body)
                    },
                )
            }
    }
}
