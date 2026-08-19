package com.sbl.sulmun2yong.global.lock.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidLockKeyExpressionException : BusinessException(ErrorCode.INVALID_LOCK_KEY_EXPRESSION)
