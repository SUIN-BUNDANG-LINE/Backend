package com.sbl.sulmun2yong.demo.service

import com.sbl.sulmun2yong.demo.dto.request.DemoCreateRequest
import com.sbl.sulmun2yong.demo.dto.response.DemoResponse
import com.sbl.sulmun2yong.demo.exception.DemoNotFoundException
import com.sbl.sulmun2yong.demo.repository.DemoRepository
import org.springframework.stereotype.Service

@Service
class DemoService(private val demoRepository: DemoRepository) {
    fun getDemo(id: Long): DemoResponse {
        val demo = demoRepository.findById(id).orElseThrow { DemoNotFoundException() }
        return DemoResponse.from(demo)
    }

    fun createDemo(demoCreateRequest: DemoCreateRequest): DemoResponse {
        val demo = demoCreateRequest.toEntity()
        return DemoResponse.from(demoRepository.save(demo))
    }
}
