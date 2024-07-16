package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.user.dto.response.UserSessionResponse
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2User(
    private val userSessionResponse: UserSessionResponse,
    private val attributes: MutableMap<String, Any>,
) : OAuth2User {
    override fun getName(): String = userSessionResponse.id

    override fun getAttributes(): MutableMap<String, Any> = attributes

    override fun getAuthorities(): MutableCollection<GrantedAuthority> {
        val authorities = SimpleGrantedAuthority(userSessionResponse.role)
        return mutableListOf(authorities)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CustomOAuth2User) {
            return false
        }
        return userSessionResponse.id == other.userSessionResponse.id
    }

    override fun hashCode(): Int = userSessionResponse.id.hashCode()
}
