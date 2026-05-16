package com.sbl.sulmun2yong.drawing.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidDrawingException : BusinessException(ErrorCode.INVALID_DRAWING)
