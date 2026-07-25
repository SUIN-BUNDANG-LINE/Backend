package com.sbl.sulmun2yong.global.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.Base64

@Configuration
class RestTemplateConfig(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
    @Value("\${ai-server.api-key}")
    private val aiServerApiKey: String,
    @Value("\${toss.base-url}")
    private val tossBaseUrl: String,
    @Value("\${toss.secret-key}")
    private val tossSecretKey: String,
) {

    @Bean
    fun tossPaymentsTemplate(): RestTemplate {
        // 토스 응답은 camelCase + 우리가 모르는 필드가 많다 -> kotlin 모듈 + unknown 무시
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

    @Bean
    fun requestToPythonServerTemplate(messageConverters: HttpMessageConverters): RestTemplate {
        val objectMapper =
            ObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
                registerKotlinModule()
            }

        return RestTemplate(listOf(MappingJackson2HttpMessageConverter(objectMapper)))
            .apply {
                uriTemplateHandler = DefaultUriBuilderFactory(aiServerBaseUrl)
                interceptors.add(
                    ClientHttpRequestInterceptor { request, body, execution ->
                        request.headers.add("x-api-key", aiServerApiKey)
                        execution.execute(request, body)
                    },
                )
            }
    }
}
