package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomAuthenticationSuccessHandler
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomLogoutSuccessHandler
import com.sbl.sulmun2yong.global.config.oauth2.strategy.CustomExpiredSessionStrategy
import com.sbl.sulmun2yong.global.config.oauth2.strategy.CustomInvalidSessionStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.web.filter.ForwardedHeaderFilter

@Configuration
class SecurityConfig(
    @Value("\${frontend.base-url}")
    private val baseUrl: String,
    @Value("\${swagger.username}")
    private val username: String?,
    @Value("\${swagger.password}")
    private val password: String?,
    private val entryPoint: AuthenticationEntryPoint,
    private val deniedHandler: AccessDeniedHandler,
) {
    @Bean
    fun sessionRegistry(): SessionRegistry = SessionRegistryImpl()

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

    @ConditionalOnProperty(prefix = "swagger", name = ["login"], havingValue = "true")
    @Order(0)
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/login")
            .csrf { it.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/swagger-ui/**")
                    .hasAnyRole("SWAGGER_USER", "ADMIN")
                    .requestMatchers("/v3/api-docs/**")
                    .hasAnyRole("SWAGGER_USER", "ADMIN")
                    .requestMatchers("/**")
                    .permitAll()
            }.formLogin(Customizer.withDefaults())

        return http.build()
    }

    @Order(1)
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        customOAuth2Service: CustomOAuth2Service,
    ): SecurityFilterChain {
        http {
            csrf {
                disable()
            }
            oauth2Login {
                userInfoEndpoint {
                    userService = customOAuth2Service
                }
                authenticationSuccessHandler = CustomAuthenticationSuccessHandler(baseUrl)
            }
            logout {
                logoutUrl = "/user/logout"
                invalidateHttpSession = false
                logoutSuccessHandler = CustomLogoutSuccessHandler(baseUrl, sessionRegistry())
            }
            authorizeHttpRequests {
                authorize("/api/v1/admin/**", hasRole("ADMIN"))
                authorize("/api/v1/user/**", authenticated)
                // TODO: 추후에 AUTHENTICATED_USER 로 수정
                authorize("/api/v1/surveys/workbench/**", hasRole("ADMIN"))
                authorize("/**", permitAll)
            }
            exceptionHandling {
                authenticationEntryPoint = entryPoint
                accessDeniedHandler = deniedHandler
            }
            sessionManagement {
                invalidSessionStrategy = CustomInvalidSessionStrategy()
                sessionConcurrency {
                    expiredSessionStrategy = CustomExpiredSessionStrategy()
                    invalidSessionStrategy = CustomInvalidSessionStrategy()
                    maximumSessions = 1
                    maxSessionsPreventsLogin = false
                    sessionRegistry = sessionRegistry()
                }
            }
        }
        return http.build()
    }

    @Bean
    fun forwardedHeaderFilter(): FilterRegistrationBean<ForwardedHeaderFilter> {
        val filterRegistrationBean = FilterRegistrationBean<ForwardedHeaderFilter>()

        filterRegistrationBean.filter = ForwardedHeaderFilter()
        filterRegistrationBean.order = Ordered.HIGHEST_PRECEDENCE

        return filterRegistrationBean
    }
}
