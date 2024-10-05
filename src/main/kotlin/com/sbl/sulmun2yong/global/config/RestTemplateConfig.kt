package com.sbl.sulmun2yong.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

// TODO : WebClient 시용 고려
@Configuration
class RestTemplateConfig(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
    private val snakeTypeObjectMapper: ObjectMapper,
) {
    @Bean
    fun requestToPythonServerTemplate(messageConverters: HttpMessageConverters) =
        RestTemplate(listOf(MappingJackson2HttpMessageConverter(snakeTypeObjectMapper)))
            .apply {
                uriTemplateHandler = DefaultUriBuilderFactory(aiServerBaseUrl)
            }
}
