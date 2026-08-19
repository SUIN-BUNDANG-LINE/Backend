package com.sbl.sulmun2yong.global.config.oauth2.provider.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class NaverAttributeCastingFailedException : BusinessException(ErrorCode.NAVER_ATTRIBUTE_CASTING_FAILED)
