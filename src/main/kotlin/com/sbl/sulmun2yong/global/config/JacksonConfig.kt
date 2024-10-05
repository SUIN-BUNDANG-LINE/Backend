package com.sbl.sulmun2yong.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    fun snakeCaseObjectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        return objectMapper
    }
}
