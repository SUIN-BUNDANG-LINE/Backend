package com.sbl.sulmun2yong.global.data.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidPhoneNumberException : BusinessException(ErrorCode.INVALID_PHONE_NUMBER)
