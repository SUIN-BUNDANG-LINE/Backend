package com.sbl.sulmun2yong.global.util.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidFileUrlException : BusinessException(ErrorCode.INVALID_FILE_URL)
