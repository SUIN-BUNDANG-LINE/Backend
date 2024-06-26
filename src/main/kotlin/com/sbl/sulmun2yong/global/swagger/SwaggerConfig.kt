package com.sbl.sulmun2yong.global.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .components(Components())
            .info(apiInfo())

    private fun apiInfo() =
        Info()
            .title("설문이용 API 문서")
            .version("1.0.0")
}
