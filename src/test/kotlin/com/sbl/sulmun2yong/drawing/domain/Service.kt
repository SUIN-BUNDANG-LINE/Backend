package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.service.DrawingBoardService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest
class Service {
    @Test
    fun makeDrawingBoard(
        @Autowired
        drawingBoardService: DrawingBoardService,
    ) {
        drawingBoardService.makeDrawingBoard(
            UUID.fromString("00363c6a-db22-4df3-b75a-2dd347c8089f"),
            boardSize = 200,
            surveyRewards =
                listOf(
                    Reward(
                        "테스트 아아",
                        "커피",
                        100,
                    ),
                ),
        )
    }
}
