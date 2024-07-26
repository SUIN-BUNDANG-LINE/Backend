package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.fixture.DrawingBoardFixtureFactory
import org.junit.jupiter.api.Test
import java.util.UUID

class DrawingTest {
    @Test
    fun `추첨을 한다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard(UUID.randomUUID())
        val selectedNumber = 3
        val drawingMachine = DrawingMachine(drawingBoard, selectedNumber)

        // when
        // then
        drawingMachine.insertQuarter()
        val result =
            if (drawingMachine.getResult()) {
                "당첨"
            } else {
                "꽝"
            }
        println(drawingBoard)
        println("${result}을 뽑았습니다. ${drawingMachine.getRewardName()}을 받으세요.")
    }
}
