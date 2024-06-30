package com.sbl.sulmun2yong.demo.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class DemoNotFoundException : BusinessException(ErrorCode.DEMO_NOT_FOUND)
