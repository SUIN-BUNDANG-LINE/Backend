package com.sbl.sulmun2yong.global.util.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidExtensionException : BusinessException(ErrorCode.INVALID_EXTENSION)
