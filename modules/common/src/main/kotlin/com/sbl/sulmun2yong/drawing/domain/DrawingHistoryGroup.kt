package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import java.util.UUID

class DrawingHistoryGroup(
    val surveyId: UUID,
    val count: Int,
    val histories: List<DrawingHistory>,
)
