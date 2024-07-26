package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class OutOfPaperState : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName() = throw InvalidDrawingException()
}
