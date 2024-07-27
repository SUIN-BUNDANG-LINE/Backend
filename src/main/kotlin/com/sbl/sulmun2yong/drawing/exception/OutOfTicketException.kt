package com.sbl.sulmun2yong.drawing.exception

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class OutOfTicketException : BusinessException(ErrorCode.OUT_OF_TICKET)
