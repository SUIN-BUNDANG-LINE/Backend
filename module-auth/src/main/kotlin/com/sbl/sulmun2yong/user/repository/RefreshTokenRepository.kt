package com.sbl.sulmun2yong.user.repository

import com.sbl.sulmun2yong.user.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Optional<RefreshTokenEntity>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    )
}
