package com.sbl.sulmun2yong.user.repository

import com.sbl.sulmun2yong.user.entity.RefreshToken
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : MongoRepository<RefreshToken, UUID> {
    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Optional<RefreshToken>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    )
}
