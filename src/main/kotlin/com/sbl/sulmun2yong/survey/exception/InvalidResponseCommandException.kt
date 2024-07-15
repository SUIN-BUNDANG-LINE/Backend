package com.sbl.sulmun2yong.survey.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidResponseCommandException : BusinessException(ErrorCode.INVALID_RESPONSE_COMMAND)
