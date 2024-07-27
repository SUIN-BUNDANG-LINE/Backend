package com.sbl.sulmun2yong.drawing.domain.state

interface State {
    fun insertQuarter()

    fun selectTicket()

    fun openTicket()

    fun getResult(): Boolean

    fun getRewardName(): String
}
