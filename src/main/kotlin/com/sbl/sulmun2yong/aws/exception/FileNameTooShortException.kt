package com.sbl.sulmun2yong.aws.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class FileNameTooShortException : BusinessException(ErrorCode.FILE_NAME_TOO_SHORT)
