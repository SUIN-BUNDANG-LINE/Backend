package com.sbl.sulmun2yong.demo.dto.request

import com.sbl.sulmun2yong.demo.entity.Demo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class DemoCreateRequest(
    @field:NotBlank
    @field:Size(max = 10)
    val title: String,
) {
    fun toEntity(): Demo {
        return Demo(title = title)
    }
}
