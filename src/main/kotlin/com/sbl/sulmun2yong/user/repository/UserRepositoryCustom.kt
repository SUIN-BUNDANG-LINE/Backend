package com.sbl.sulmun2yong.user.repository

interface UserRepositoryCustom {
    fun countByNicknameRegex(nicknameRegex: String): Long
}
