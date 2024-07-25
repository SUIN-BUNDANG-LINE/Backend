package com.sbl.sulmun2yong.drawing

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import java.util.UUID

class DrawingService {
    fun checkHasQuarter(
        participantId: UUID,
        surveyId: UUID,
    ): Boolean {
        if (participantId.toString() == "00363c6a-db22-4df3-b75a-2dd347c8089f") {
            return true
        }
        return false
    }

    fun draw(selectedNumber: Int) {
        DrawingMachine(selectedNumber)
    }
}
