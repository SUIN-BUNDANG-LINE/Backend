package com.sbl.sulmun2yong.global.oauth2

import com.sbl.sulmun2yong.global.oauth2.strategy.OAuth2Strategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OAuth2Module {
    @Autowired
    constructor(
        oAuth2StrategyFactory: Map<String, OAuth2Strategy>,
    ) {
        fun getSocialLoginUrl(provider: String): String {
            val strategy = oAuth2StrategyFactory[provider]
            if (strategy != null) {
                return strategy.getRedirectURL()
            }
            return ""
        }

        fun getAccessToken(
            code: String,
            provider: String,
        ): String {
            val strategy = oAuth2StrategyFactory[provider]
            if (strategy != null) {
                return strategy.getAccessToken(code)
            }
            return ""
        }

        fun socialLogin(provider: String) {
            val strategy = oAuth2StrategyFactory[provider]

            val socialLoginUserData = strategy?.getSocialLoginUserData()

            val loginToken = socialLoginUserData?.let { strategy.login(it) }
        }
    }
}
