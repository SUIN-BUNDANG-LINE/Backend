package com.sbl.sulmun2yong.ai.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class TextTooLongException : BusinessException(ErrorCode.TEXT_TOO_LONG)
