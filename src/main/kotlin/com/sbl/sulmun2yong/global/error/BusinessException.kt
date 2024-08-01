package com.sbl.sulmun2yong.global.error

open class BusinessException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
