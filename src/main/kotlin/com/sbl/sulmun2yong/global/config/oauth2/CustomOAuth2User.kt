package com.sbl.sulmun2yong.global.config.oauth2

import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.dto.DefaultUserProfile
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.UUID

data class CustomOAuth2User(
    private val id: UUID,
    private val role: UserRole,
    private val nickname: String,
    // TODO: attributes 통일
    private val attributes: MutableMap<String, Any>,
) : OAuth2User {
    override fun getName(): String = id.toString()

    override fun getAttributes(): MutableMap<String, Any> = attributes

    override fun getAuthorities(): MutableCollection<GrantedAuthority> {
        val authorities = SimpleGrantedAuthority(role.name)
        return mutableListOf(authorities)
    }

    fun getDefaultUserProfile(): DefaultUserProfile =
        DefaultUserProfile(
            id = id,
            nickname = nickname,
        )
}
