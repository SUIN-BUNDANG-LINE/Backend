package com.sbl.sulmun2yong.demo.controller

import com.sbl.sulmun2yong.demo.controller.doc.DemoApiDoc
import com.sbl.sulmun2yong.demo.entity.Demo
import com.sbl.sulmun2yong.demo.service.DemoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/demo")
class DemoController(private val demoService: DemoService) : DemoApiDoc {
    @GetMapping("/{id}")
    override fun getDemo(
        @PathVariable id: Long,
    ): Demo {
        return demoService.getDemo(id)
    }

    @PostMapping
    override fun createDemo(
        @RequestBody demo: Demo,
    ): Long {
        return demoService.createDemo(demo)
    }
}
