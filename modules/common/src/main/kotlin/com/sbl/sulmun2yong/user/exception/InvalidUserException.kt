package com.sbl.sulmun2yong.user.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidUserException : BusinessException(ErrorCode.INVALID_USER_EXCEPTION)
