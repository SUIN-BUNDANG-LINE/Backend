package com.sbl.sulmun2yong.drawing.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class OutOfPaperException : BusinessException(ErrorCode.INVALID_DRAWING)
