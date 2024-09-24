package com.sbl.sulmun2yong.ai.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class FileNotFoundException : BusinessException(ErrorCode.FILE_NOT_FOUND)
