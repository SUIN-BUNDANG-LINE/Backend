package com.sbl.sulmun2yong.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    @Value("\${frontend.base-url}")
    private val baseUrl: String,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOrigins(baseUrl) // 프론트엔드 도메인
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true)
            .allowedHeaders("*")
    }
}
