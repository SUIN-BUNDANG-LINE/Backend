package com.sbl.sulmun2yong.aws.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class OutOfFileSizeException : BusinessException(ErrorCode.OUT_OF_FILE_SIZE)
