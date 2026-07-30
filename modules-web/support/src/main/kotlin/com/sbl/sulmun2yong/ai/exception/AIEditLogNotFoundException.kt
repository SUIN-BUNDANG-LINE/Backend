package com.sbl.sulmun2yong.ai.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class AIEditLogNotFoundException : BusinessException(ErrorCode.AI_EDIT_LOG_NOT_FOUND)
