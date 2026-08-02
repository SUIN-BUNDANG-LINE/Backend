package com.sbl.sulmun2yong.cofunding.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class CoFundingNotFoundException : BusinessException(ErrorCode.CO_FUNDING_NOT_FOUND)
