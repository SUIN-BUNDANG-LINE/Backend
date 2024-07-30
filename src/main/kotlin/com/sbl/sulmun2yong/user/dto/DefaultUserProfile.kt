package com.sbl.sulmun2yong.user.dto

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Base64
import java.util.UUID

data class DefaultUserProfile(
    val id: UUID,
    val nickname: String,
) {
    fun toBase64Json(): String {
        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(this)
        return Base64.getEncoder().encodeToString(json.toByteArray())
    }
}
