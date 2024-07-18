package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import com.sbl.sulmun2yong.global.config.oauth2.strategy.CustomExpiredSessionStrategy
import com.sbl.sulmun2yong.global.config.oauth2.strategy.CustomInvalidSessionStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint

@Configuration
class SecurityConfig {
    @Bean
    fun sessionRegistry(): SessionRegistry = SessionRegistryImpl()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        customOAuth2Service: CustomOAuth2Service,
    ): SecurityFilterChain {
        http {
            csrf {
                disable()
            }
            authorizeHttpRequests {
                authorize("/api/v1/admin/**", hasRole("ADMIN"))
                authorize("/api/v1/user/**", authenticated)
                authorize("/**", permitAll)
            }
            oauth2Login {
                loginPage = "/user/login"
                userInfoEndpoint {
                    userService = customOAuth2Service
                }
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            logout {
                logoutUrl = "/user/logout"
                invalidateHttpSession = false
                logoutSuccessHandler = CustomLogoutSuccessHandler(sessionRegistry())
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
