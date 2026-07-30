package com.sbl.sulmun2yong.user.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class UserNotFoundException : BusinessException(ErrorCode.USER_NOT_FOUND)
