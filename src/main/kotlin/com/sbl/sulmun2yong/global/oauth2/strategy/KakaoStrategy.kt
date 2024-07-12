package com.sbl.sulmun2yong.global.oauth2.strategy

import com.sbl.sulmun2yong.global.oauth2.SocialLoginUserData
import org.springframework.stereotype.Component

@Component
class KakaoStrategy : OAuth2Strategy {
    override fun getRedirectURL(): String {
        TODO("Not yet implemented")
    }

    override fun getAccessToken(code: String): String {
        TODO("Not yet implemented")
    }

    override fun getSocialLoginUserData(): SocialLoginUserData {
        TODO("Not yet implemented")
    }

    override fun login(socialLoginUserData: SocialLoginUserData): String {
        TODO("Not yet implemented")
    }
}
