package com.sbl.sulmun2yong.drawing.dto

import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import java.util.UUID

data class DrawingHistoryDTOGroupedBySurveyId(
    val id: UUID,
    val count: Int,
    val items: List<DrawingHistory>,
)
