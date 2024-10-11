package com.sbl.sulmun2yong.user.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import java.util.UUID

// 빠른 구현을 위해서 & 도메인 클래스라고 보기에는 애매하여 entity 패키지에 위치시킴
@Document(collection = "refreshTokens")
class RefreshToken private constructor(
    @Id
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expirationDate: Date,
) : BaseTimeDocument() {
    companion object {
        fun of(
            id: UUID,
            userId: UUID,
            token: String,
            refreshTokenExpiration: Long,
        ): RefreshToken =
            RefreshToken(
                id,
                userId,
                token,
                Date(System.currentTimeMillis() + refreshTokenExpiration),
            )
    }
}
