package com.sbl.sulmun2yong.drawing.domain

import java.util.UUID

data class Reward(
    val id: UUID,
    val name: String,
    val category: String,
    val count: Int,
)
