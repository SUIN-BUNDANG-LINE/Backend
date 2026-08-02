package com.sbl.sulmun2yong.global.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

data class AuthClaims(
    val userId: String,
    val role: String,
)

@Component
class JwtValidator(
    @Value("\${jwt.secret-key}")
    secretKey: String,
) {
    private val signingKey: SecretKey =
        Keys.hmacShaKeyFor(Base64.getUrlDecoder().decode(secretKey))

    // 유효한 access 토큰이면 subject(userId 문자열) 반환, 무효/만료면 null.
    fun extractUserId(token: String): AuthClaims? {
        val claims = parse(token) ?: return null
        val notExpired = claims.expiration?.after(Date()) ?: false
        if (!notExpired) return null
        val userId = claims.subject ?: return null
        val role = claims.get("role", String::class.java) ?: return null
        return AuthClaims(userId, role)
    }

    private fun parse(token: String): Claims? =
        try {
            Jwts
                .parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
}
