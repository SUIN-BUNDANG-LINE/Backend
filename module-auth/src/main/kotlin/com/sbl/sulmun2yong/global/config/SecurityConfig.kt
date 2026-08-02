package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import com.sbl.sulmun2yong.global.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomAuthenticationSuccessHandler
import com.sbl.sulmun2yong.global.config.oauth2.handler.CustomLogoutSuccessHandler
import com.sbl.sulmun2yong.global.jwt.JwtTokenProvider
import com.sbl.sulmun2yong.user.repository.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.ForwardedHeaderFilter

// auth-service 보안 설정 — "발급" 전담.
// OAuth2 로그인·JWT 발급·리프레시·로그아웃만 담당한다.
// 게이트웨이 헤더 신뢰(HeaderAuthenticationFilter)·우회 차단(GatewayOnlyFilter)은
// 도메인 서비스(web) 몫이므로 여기엔 없다 — auth 는 로그인 경로 자체가 공개다.
@Configuration
class SecurityConfig(
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
    @Value("\${backend.base-url}")
    private val backendBaseUrl: String,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    @Bean
    fun cookieAuthorizationRequestRepository(): HttpCookieOAuth2AuthorizationRequestRepository =
        HttpCookieOAuth2AuthorizationRequestRepository()

    @Bean
    fun forwardedHeaderFilter(): FilterRegistrationBean<ForwardedHeaderFilter> {
        val filterRegistrationBean = FilterRegistrationBean<ForwardedHeaderFilter>()

        filterRegistrationBean.filter = ForwardedHeaderFilter()
        filterRegistrationBean.order = Ordered.HIGHEST_PRECEDENCE

        return filterRegistrationBean
    }

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
                        refreshTokenRepository,
                    )
            }
            logout {
                logoutUrl = "/user/logout"
                logoutSuccessHandler =
                    CustomLogoutSuccessHandler(
                        frontendBaseUrl,
                        jwtTokenProvider,
                        refreshTokenRepository,
                    )
            }
            authorizeHttpRequests {
                authorize("/**", permitAll)
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
        }
        return http.build()
    }
}
