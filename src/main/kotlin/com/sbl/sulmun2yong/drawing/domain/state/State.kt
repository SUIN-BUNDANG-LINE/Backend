package com.sbl.sulmun2yong.drawing.domain.state

interface State {
    fun insertQuarter()

    fun getResult(): Boolean

    fun getRewardName(): String
}
