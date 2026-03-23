package com.sbl.sulmun2yong.user.repository

import com.sbl.sulmun2yong.global.config.oauth2.provider.Provider
import com.sbl.sulmun2yong.user.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): Optional<UserEntity>
}
