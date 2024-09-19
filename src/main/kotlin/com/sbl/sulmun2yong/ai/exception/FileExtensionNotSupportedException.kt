package com.sbl.sulmun2yong.ai.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class FileExtensionNotSupportedException : BusinessException(ErrorCode.FILE_EXTENSION_NOT_SUPPORTED)
