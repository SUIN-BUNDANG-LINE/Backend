package com.sbl.sulmun2yong.drawing.controller.doc

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/api/v1/drawing")
class DrawingBoardController {
    @GetMapping("/")
    fun draw(): String = "draw"
}
