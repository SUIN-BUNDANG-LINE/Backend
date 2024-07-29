package com.sbl.sulmun2yong.drawing.service

import com.sbl.sulmun2yong.survey.domain.Reward
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest
class DrawingBoardServiceTest {
    @Autowired
    private lateinit var drawingBoardService: DrawingBoardService // 테스트할 서비스 클래스

    @Test
    fun makeDrawingBoard() {
        // given
        val surveyId = UUID.fromString("00363c6a-db22-4df3-b75a-2dd347c8089f")
        val boardSize = 200
        val surveyRewards =
            listOf(
                Reward(
                    id = UUID.randomUUID(),
                    name = "테스트 아이스 아메리카노",
                    category = "음료",
                    count = 100,
                ),
                Reward(
                    id = UUID.randomUUID(),
                    name = "테스트 아이스 라떼",
                    category = "음료",
                    count = 100,
                ),
            )

        // when
        drawingBoardService.makeDrawingBoard(surveyId, boardSize, surveyRewards)
    }
}
