package com.sbl.sulmun2yong.drawing.controller

import com.sbl.sulmun2yong.drawing.dto.request.DrawingRequest
import com.sbl.sulmun2yong.drawing.dto.response.DrawingResultResponse
import com.sbl.sulmun2yong.drawing.service.DrawingBoardService
import com.sbl.sulmun2yong.drawing.service.strategy.DrawMode
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 경쟁 제어 4방식 비교 측정 전용 진입점.
 *
 * 운영 경로(`/draw`, Redisson)는 `DrawingBoardController` 에 남기고, 나머지 셋은 여기로 분리해
 * `lock-experiment.enabled=true` 일 때만 라우팅에 등록한다. JVM 로컬 경로 등이 운영에서
 * 호출 가능하면 같은 티켓 중복 배정이나 한 사람의 중복 참여를 막을 장치가 없다.
 *
 * 측정 코드는 영구 보존한다 — 다섯 방식의 대조가 이 프로젝트의 산출물이기 때문이다.
 * 다만 보존과 노출은 다른 문제라, 코드는 남기고 경로만 닫는다.
 */
@RestController
@RequestMapping("/api/v1/drawing-board")
@ConditionalOnProperty(prefix = "lock-experiment", name = ["enabled"], havingValue = "true")
class DrawingExperimentController(
    private val drawingBoardService: DrawingBoardService,
) {
    @PostMapping("/draw-default")
    fun doDrawingWithDefault(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> = draw(DrawMode.DEFAULT, request)

    @PostMapping("/draw-serializable")
    fun doDrawingWithSerializable(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> = draw(DrawMode.SERIALIZABLE, request)

    @PostMapping("/draw-synchronized")
    fun doDrawingWithSynchronized(
        @RequestBody request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> = draw(DrawMode.SYNCHRONIZED, request)

    private fun draw(
        mode: DrawMode,
        request: DrawingRequest,
    ): ResponseEntity<DrawingResultResponse> =
        ResponseEntity.ok(
            drawingBoardService.doDrawing(
                mode = mode,
                participantId = request.participantId,
                selectedNumber = request.selectedNumber,
                phoneNumber = request.phoneNumber,
            ),
        )
}
