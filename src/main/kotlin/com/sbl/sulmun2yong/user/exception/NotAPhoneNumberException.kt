package com.sbl.sulmun2yong.user.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class NotAPhoneNumberException : BusinessException(ErrorCode.NOT_A_PHONE_NUMBER)
