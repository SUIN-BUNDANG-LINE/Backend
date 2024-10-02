package com.sbl.sulmun2yong.survey.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidFinishedAtException : BusinessException(ErrorCode.INVALID_FINISHED_AT)
