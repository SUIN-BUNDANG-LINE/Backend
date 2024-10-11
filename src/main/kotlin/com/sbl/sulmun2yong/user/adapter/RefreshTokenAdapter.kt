package com.sbl.sulmun2yong.user.adapter

import com.sbl.sulmun2yong.user.entity.RefreshToken
import com.sbl.sulmun2yong.user.repository.RefreshTokenRepository
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Component
class RefreshTokenAdapter(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    fun save(userRefreshToken: RefreshToken) {
        refreshTokenRepository.save(userRefreshToken)
    }

    fun findByTokenIdAndUserId(
        tokenId: UUID,
        userId: UUID,
    ): RefreshToken? = refreshTokenRepository.findByIdAndUserId(tokenId, userId).getOrNull()

    fun deleteByTokenIdAndUserId(
        tokenId: UUID,
        userId: UUID,
    ) {
        refreshTokenRepository.deleteByIdAndUserId(tokenId, userId)
    }
}
