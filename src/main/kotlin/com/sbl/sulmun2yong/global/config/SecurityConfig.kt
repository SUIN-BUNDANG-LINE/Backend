package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val customOAuth2Service: CustomOAuth2Service,
) {
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
                loginPage = "/frontend/login"
                userInfoEndpoint {
                    userService = customOAuth2Service
                }
            }
            logout {
                logoutUrl = "/user/logout"
                invalidateHttpSession = false
                logoutSuccessHandler = CustomLogoutSuccessHandler(sessionRegistry())
            }
            sessionManagement {
                invalidSessionUrl = "/frontend/invalid-session"
                sessionConcurrency {
                    expiredUrl = "/frontend/expired"
                    invalidSessionUrl = "/frontend/invalid-session"
                    maximumSessions = 1
                    maxSessionsPreventsLogin = false
                    sessionRegistry = sessionRegistry()
                }
            }
        }
        return http.build()
    }
}
