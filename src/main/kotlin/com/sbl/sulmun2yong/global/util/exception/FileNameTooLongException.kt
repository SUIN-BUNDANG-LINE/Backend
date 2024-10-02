package com.sbl.sulmun2yong.global.util.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class FileNameTooLongException : BusinessException(ErrorCode.FILE_NAME_TOO_LONG)
