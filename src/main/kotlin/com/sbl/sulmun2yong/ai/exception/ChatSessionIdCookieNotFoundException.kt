package com.sbl.sulmun2yong.ai.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class ChatSessionIdCookieNotFoundException : BusinessException(ErrorCode.CHAT_SESSION_ID_COOKIE_NOT_FOUND)
