package com.sbl.sulmun2yong.global.config

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val customOAuth2Service: CustomOAuth2Service,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.disable()
            }.authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/v1/oauth2/**")
                    .hasRole("ADMIN")
                authorize
                    .requestMatchers("/frontend/user/**")
                    .authenticated()
                authorize
                    .anyRequest()
                    .permitAll()
            }.oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/frontend/loginForm")
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2Service)
                }
                oauth2.defaultSuccessUrl("/frontend", true)
            }
        return http.build()
    }
}
