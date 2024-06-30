package com.sbl.sulmun2yong.demo.dto.response

import com.sbl.sulmun2yong.demo.entity.Demo

data class DemoResponse private constructor(
    val id: Long,
    val title: String,
) {
    companion object {
        fun from(demo: Demo): DemoResponse {
            return DemoResponse(demo.id, demo.title)
        }
    }
}
