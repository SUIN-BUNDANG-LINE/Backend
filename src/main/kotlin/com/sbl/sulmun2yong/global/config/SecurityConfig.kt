package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.session.HttpSessionEventPublisher

@Configuration
class SecurityConfig(
    private val customOAuth2Service: CustomOAuth2Service,
) {
    @Bean
    fun httpSessionEventPublisher(): HttpSessionEventPublisher = HttpSessionEventPublisher()

    @Bean
    fun sessionRegistry(): SessionRegistry = SessionRegistryImpl()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf {
                disable()
            }
            authorizeHttpRequests {
                authorize("/admin/**", hasRole("ADMIN"))
                authorize("/user/**", authenticated)
                authorize("/**", permitAll)
            }
            oauth2Login {
                loginPage = "/frontend/loginForm"
                userInfoEndpoint {
                    userService = customOAuth2Service
                }
                defaultSuccessUrl("/frontend", true)
            }
            logout {
                logoutUrl = "/api/v1/oauth2/logout"
                logoutSuccessUrl = "/frontend"
            }
            sessionManagement {
                invalidSessionUrl = "/frontend"
                sessionConcurrency {
                    maximumSessions = 2
                    maxSessionsPreventsLogin = false
                    sessionRegistry = sessionRegistry()
                }
            }
        }
        return http.build()
    }
}
