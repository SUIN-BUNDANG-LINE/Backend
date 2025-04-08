package com.sbl.sulmun2yong.global.config

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

// TODO : WebClient 시용 고려
@Configuration
class RestTemplateConfig(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
    @Value("\${ai-server.api-key}")
    private val aiServerApiKey: String,
) {
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
