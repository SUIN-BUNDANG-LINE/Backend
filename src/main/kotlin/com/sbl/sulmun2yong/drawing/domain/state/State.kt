package com.sbl.sulmun2yong.drawing.domain.state

interface State {
    fun insertQuarter()

    fun selectPaper()

    fun openPaper()

    fun getResult(): Boolean

    fun getRewardName(): String
}
