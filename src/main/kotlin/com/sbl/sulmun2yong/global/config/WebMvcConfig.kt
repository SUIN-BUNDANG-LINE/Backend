package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.resolver.IsAdminArgumentResolver
import com.sbl.sulmun2yong.global.resolver.LoginUserArgumentResolver
import com.sbl.sulmun2yong.global.resolver.NullableLoginUserArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
    private val loginUserArgumentResolver: LoginUserArgumentResolver,
    private val isAdminArgumentResolver: IsAdminArgumentResolver,
    private val nullableLoginUserArgumentResolver: NullableLoginUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOrigins(frontendBaseUrl) // 프론트엔드 도메인
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowCredentials(true)
            .allowedHeaders("*")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginUserArgumentResolver)
        resolvers.add(isAdminArgumentResolver)
        resolvers.add(nullableLoginUserArgumentResolver)
    }
}
