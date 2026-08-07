package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.filter.GatewayOnlyFilter
import com.sbl.sulmun2yong.global.filter.HeaderAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.ForwardedHeaderFilter

// web(도메인 서비스) 보안 설정 — PURE.
// JWT 발급·OAuth2 로그인은 auth-service 로 분리됐다. web 은 게이트웨이가 검증·주입한
// X-User-Id/X-User-Role 헤더만 신뢰(HeaderAuthenticationFilter)하고, 게이트웨이 우회
// 직접 접근을 GatewayOnlyFilter 로 막는다. JWT 를 직접 다루지 않는다.
@Configuration
class SecurityConfig(
    @Value("\${swagger.username}")
    private val username: String?,
    @Value("\${swagger.password}")
    private val password: String?,
    private val entryPoint: AuthenticationEntryPoint,
    private val deniedHandler: AccessDeniedHandler,
    private val headerAuthenticationFilter: HeaderAuthenticationFilter,
    private val gatewayOnlyFilter: GatewayOnlyFilter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(): UserDetailsService {
        val user =
            User
                .builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("SWAGGER_USER")
                .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun forwardedHeaderFilter(): FilterRegistrationBean<ForwardedHeaderFilter> {
        val filterRegistrationBean = FilterRegistrationBean<ForwardedHeaderFilter>()

        filterRegistrationBean.filter = ForwardedHeaderFilter()
        filterRegistrationBean.order = Ordered.HIGHEST_PRECEDENCE

        return filterRegistrationBean
    }

    @ConditionalOnProperty(prefix = "swagger", name = ["login"], havingValue = "true")
    @Order(0)
    @Bean
    fun formLoginFilterChain(
        http: HttpSecurity,
        requestMatcherProvider: RequestMatcherProvider,
    ): SecurityFilterChain {
        http {
            csrf { disable() }
            securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/login")
            authorizeHttpRequests {
                authorize("/swagger-ui/**", hasAnyRole("SWAGGER_USER", "ADMIN"))
                authorize("/v3/api-docs/**", hasAnyRole("SWAGGER_USER", "ADMIN"))
                authorize("/**", permitAll)
            }
            formLogin {}
        }
        return http.build()
    }

    @Order(1)
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf {
                disable()
            }
            authorizeHttpRequests {
                authorize("/api/v1/admin/**", hasRole("ADMIN"))
                authorize("/api/v1/user/**", authenticated)
                authorize("/api/v1/surveys/my-page", authenticated)
                authorize("/api/v1/ai/chat/**", authenticated)
                authorize("/api/v1/ai/generate/survey/**", authenticated)
                authorize("/api/v1/surveys/workbench/**", authenticated)
                authorize("/**", permitAll)
            }
            exceptionHandling {
                authenticationEntryPoint = entryPoint
                accessDeniedHandler = deniedHandler
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(headerAuthenticationFilter)
            addFilterBefore<HeaderAuthenticationFilter>(gatewayOnlyFilter)
        }
        return http.build()
    }
}
