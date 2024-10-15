package com.sbl.sulmun2yong.global.jwt.exception

class InvalidRefreshTokenException(
    override val message: String,
) : RuntimeException(message)
