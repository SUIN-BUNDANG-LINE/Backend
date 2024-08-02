package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomAuthenticationSuccessHandler
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomLogoutSuccessHandler
import com.sbl.sulmun2yong.global.config.oauth2.strategy.CustomExpiredSessionStrategy
import com.sbl.sulmun2yong.global.config.oauth2.strategy.CustomInvalidSessionStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
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
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint

@Configuration
class SecurityConfig(
    @Value("\${frontend.base-url}")
    private val baseUrl: String,
    @Value("\${swagger.username}")
    private val username: String?,
    @Value("\${swagger.password}")
    private val password: String?,
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
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/swagger-ui/**")
                    .hasRole("SWAGGER_USER")
                    .requestMatchers("/v3/api-docs/**")
                    .hasRole("SWAGGER_USER")
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
                authorize("/**", permitAll)
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
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
}
