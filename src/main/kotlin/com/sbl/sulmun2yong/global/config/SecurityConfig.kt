package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import com.sbl.sulmun2yong.global.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomAuthenticationSuccessHandler
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomLogoutSuccessHandler
import com.sbl.sulmun2yong.global.jwt.JwtAuthenticationFilter
import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.user.adapter.RefreshTokenAdapter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
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

@Configuration
class SecurityConfig(
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
    @Value("\${backend.base-url}")
    private val backendBaseUrl: String,
    @Value("\${swagger.username}")
    private val username: String?,
    @Value("\${swagger.password}")
    private val password: String?,
    private val entryPoint: AuthenticationEntryPoint,
    private val deniedHandler: AccessDeniedHandler,
    private val jwtTokenProvider: JwtTokenProvider,
    @Lazy private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val refreshTokenAdapter: RefreshTokenAdapter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun cookieAuthorizationRequestRepository(): HttpCookieOAuth2AuthorizationRequestRepository =
        HttpCookieOAuth2AuthorizationRequestRepository()

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
    fun securityFilterChain(
        http: HttpSecurity,
        customOAuth2Service: CustomOAuth2Service,
    ): SecurityFilterChain {
        http {
            csrf {
                disable()
            }
            oauth2Login {
                authorizationEndpoint {
                    baseUri = "/oauth2/authorization"
                    authorizationRequestRepository = cookieAuthorizationRequestRepository()
                }
                userInfoEndpoint {
                    userService = customOAuth2Service
                }
                authenticationSuccessHandler =
                    CustomAuthenticationSuccessHandler(
                        frontendBaseUrl,
                        backendBaseUrl,
                        jwtTokenProvider,
                        cookieAuthorizationRequestRepository(),
                        refreshTokenAdapter,
                    )
            }
            logout {
                logoutUrl = "/user/logout"
                logoutSuccessHandler =
                    CustomLogoutSuccessHandler(
                        frontendBaseUrl,
                        jwtTokenProvider,
                        refreshTokenAdapter,
                    )
            }
            authorizeHttpRequests {
                authorize("/api/v1/admin/**", hasRole("ADMIN"))
                authorize("/api/v1/user/**", authenticated)
                authorize("/api/v1/surveys/my-page", authenticated)
                authorize("/api/v1/surveys/results/**", authenticated)
                authorize("/api/v1/s3/**", authenticated)
                authorize("/api/v1/ai/**", authenticated)
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
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }
}
