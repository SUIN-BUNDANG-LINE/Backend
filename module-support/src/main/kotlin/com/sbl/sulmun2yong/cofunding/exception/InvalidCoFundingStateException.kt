package com.sbl.sulmun2yong.cofunding.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidCoFundingStateException : BusinessException(ErrorCode.INVALID_CO_FUNDING_STATE)
