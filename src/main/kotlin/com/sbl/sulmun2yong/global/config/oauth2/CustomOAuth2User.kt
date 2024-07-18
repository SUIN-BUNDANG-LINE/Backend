package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.user.domain.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.UUID

class CustomOAuth2User(
    private val id: UUID,
    private val role: UserRole,
    // TODO: attributes 통일
    private val attributes: MutableMap<String, Any>,
) : OAuth2User {
    override fun getName(): String = id.toString()

    override fun getAttributes(): MutableMap<String, Any> = attributes

    override fun getAuthorities(): MutableCollection<GrantedAuthority> {
        val authorities = SimpleGrantedAuthority(role.name)
        return mutableListOf(authorities)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CustomOAuth2User) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
