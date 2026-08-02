package com.sbl.sulmun2yong.global.lock.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class TooManyLockRequestException : BusinessException(ErrorCode.TOO_MANY_LOCK_REQUEST)
