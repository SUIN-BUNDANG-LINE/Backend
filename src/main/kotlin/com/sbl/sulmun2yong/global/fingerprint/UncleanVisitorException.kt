package com.sbl.sulmun2yong.global.fingerprint

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class UncleanVisitorException : BusinessException(ErrorCode.UNCLEAN_VISITOR)
