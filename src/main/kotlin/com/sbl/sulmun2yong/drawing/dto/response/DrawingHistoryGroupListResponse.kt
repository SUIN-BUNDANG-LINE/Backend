package com.sbl.sulmun2yong.drawing.dto.response

import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup

class DrawingHistoryGroupListResponse(
    val count: Int,
    val DrawingHistoryGroupList: List<DrawingHistoryGroupResponse>,
) {
    companion object {
        fun of(drawingHistoryGroupList: List<DrawingHistoryGroup>) =
            DrawingHistoryGroupListResponse(
                count = drawingHistoryGroupList.size,
                DrawingHistoryGroupList =
                    drawingHistoryGroupList.map {
                        DrawingHistoryGroupResponse.of(it)
                    },
            )
    }
}
