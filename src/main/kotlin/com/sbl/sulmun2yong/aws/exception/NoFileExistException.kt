package com.sbl.sulmun2yong.aws.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class NoFileExistException : BusinessException(ErrorCode.NO_FILE_EXIST)
