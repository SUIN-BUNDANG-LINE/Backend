package com.sbl.sulmun2yong.user.dto.response

class UserSessionsResponse(
    val name: String = "",
) {
    companion object {
        fun of() = UserSessionsResponse()
    }
}
