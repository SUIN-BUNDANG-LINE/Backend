package com.sbl.sulmun2yong.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.view.MustacheViewResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
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

    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        val resolver = MustacheViewResolver()
        resolver.setCharset("UTF-8")
        resolver.setContentType("text/html; charset=UTF-8")
        resolver.setPrefix("classpath:/templates/")
        resolver.setSuffix(".html")

        registry.viewResolver(resolver)
    }
}
