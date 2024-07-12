package com.sbl.sulmun2yong.global.oauth2.strategy

import com.sbl.sulmun2yong.global.oauth2.SocialLoginUserData

interface OAuth2Strategy {
    fun getRedirectURL(): String

    fun getAccessToken(code: String): String

    fun getSocialLoginUserData(): SocialLoginUserData

    fun login(socialLoginUserData: SocialLoginUserData): String
}
