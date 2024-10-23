package com.sbl.sulmun2yong.global.jwt

import com.sbl.sulmun2yong.global.config.oauth2.CustomOAuth2User
import com.sbl.sulmun2yong.user.domain.UserRole
import com.sbl.sulmun2yong.user.dto.DefaultUserProfile
import com.sbl.sulmun2yong.user.entity.RefreshToken
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret-key}")
    private val secretKey: String,
    @Value("\${jwt.access-token-expire-time}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expire-time}")
    private val refreshTokenExpiration: Long,
    @Value("\${cookie.domain}")
    private val cookieDomain: String,
) {
    private val decodedSecretKey = Base64.getUrlDecoder().decode(secretKey)
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(decodedSecretKey)

    companion object {
        private const val ACCESS_TOKEN_COOKIE = "access-token"
        const val REFRESH_TOKEN_COOKIE = "refresh-token"
        private const val ROLE = "role"
        private const val NICKNAME = "nickname"
        private const val TOKEN_ID = "tokenId"
    }

    fun createAccessToken(profile: DefaultUserProfile): String {
        val claims =
            Jwts
                .claims()
                .add(ROLE, profile.role)
                .add(NICKNAME, profile.nickname)
                .build()
        return createToken(profile.id, accessTokenExpiration, claims)
    }

    fun createRefreshToken(
        userId: UUID,
        tokenId: UUID,
    ): String {
        val claims = Jwts.claims().add(TOKEN_ID, tokenId.toString()).build()
        return createToken(userId, refreshTokenExpiration, claims)
    }

    fun getUserIdFromToken(token: String): UUID? = getClaimsFromToken(token)?.let { UUID.fromString(it.subject) }

    fun getTokenIdFromToken(token: String): UUID? = getClaimsFromToken(token)?.let { UUID.fromString(it.get(TOKEN_ID, String::class.java)) }

    fun getUserFromToken(token: String): CustomOAuth2User? {
        val claims = getClaimsFromToken(token)
        return if (claims != null) {
            CustomOAuth2User(
                UUID.fromString(claims.subject),
                UserRole.valueOf(claims.get(ROLE, String::class.java)),
                claims.get(NICKNAME, String::class.java),
                mutableMapOf(),
            )
        } else {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        val claims: Claims? = getClaimsFromToken(token)
        return claims?.expiration?.after(Date()) ?: false
    }

    fun makeAccessTokenCookie(accessToken: String): Cookie = makeCookie(ACCESS_TOKEN_COOKIE, accessToken, accessTokenExpiration)

    fun makeExpiredAccessTokenCookie(): Cookie = makeCookie(ACCESS_TOKEN_COOKIE, "", 0)

    fun makeRefreshTokenCookie(refreshToken: String): Cookie = makeCookie(REFRESH_TOKEN_COOKIE, refreshToken, refreshTokenExpiration)

    fun makeExpiredRefreshTokenCookie(): Cookie = makeCookie(REFRESH_TOKEN_COOKIE, "", 0)

    fun makeRefreshTokenDocument(
        tokenId: UUID,
        userId: UUID,
        refreshToken: String,
    ) = RefreshToken.of(tokenId, userId, refreshToken, refreshTokenExpiration)

    private fun makeCookie(
        name: String,
        value: String,
        expiration: Long,
    ): Cookie {
        val cookie = Cookie(name, value)
        cookie.path = "/"
        if (cookieDomain != "localhost") cookie.domain = cookieDomain
        cookie.isHttpOnly = true
        cookie.maxAge = (expiration / 1000).toInt()
        cookie.setAttribute("SameSite", "Lax")
        return cookie
    }

    private fun createToken(
        userId: UUID,
        expiration: Long,
        claims: Claims,
    ): String {
        val now = Date()
        val expirationDate = Date(now.time + expiration)

        return Jwts
            .builder()
            .subject(userId.toString())
            .claims(claims)
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    private fun getClaimsFromToken(token: String): Claims? =
        try {
            Jwts
                .parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            e.claims
        } catch (e: Exception) {
            null
        }
}
