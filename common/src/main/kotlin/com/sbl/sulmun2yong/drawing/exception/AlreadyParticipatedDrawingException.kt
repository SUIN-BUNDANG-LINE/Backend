package com.sbl.sulmun2yong.drawing.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class AlreadyParticipatedDrawingException : BusinessException(ErrorCode.ALREADY_PARTICIPATED_DRAWING)
