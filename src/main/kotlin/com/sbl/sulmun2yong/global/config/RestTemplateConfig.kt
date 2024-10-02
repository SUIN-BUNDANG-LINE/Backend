package com.sbl.sulmun2yong.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

// TODO : WebClient 시용 고려
@Configuration
class RestTemplateConfig {
    @Bean
    fun createRestTemplate(): RestTemplate = RestTemplate()
}
