package com.sbl.sulmun2yong.demo.controller.doc

import com.sbl.sulmun2yong.demo.entity.Demo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Demo", description = "Demo API")
interface DemoApiDoc {
    @Operation(summary = "Demo 조회")
    fun getDemo(
        @PathVariable id: Long,
    ): Demo

    @Operation(summary = "Demo 생성")
    fun createDemo(
        @RequestBody demo: Demo,
    ): Long
}
