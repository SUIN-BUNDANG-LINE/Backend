package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    val token: String,
    @Column(nullable = false)
    val expirationDate: Date,
) : BaseTimeEntity() {
    companion object {
        fun of(
            id: UUID,
            userId: UUID,
            token: String,
            refreshTokenExpiration: Long,
        ): RefreshTokenEntity =
            RefreshTokenEntity(
                id,
                userId,
                token,
                Date(System.currentTimeMillis() + refreshTokenExpiration),
            )
    }
}
