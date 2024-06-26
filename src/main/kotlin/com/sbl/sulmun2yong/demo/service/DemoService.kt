package com.sbl.sulmun2yong.demo.service

import com.sbl.sulmun2yong.demo.entity.Demo
import com.sbl.sulmun2yong.demo.repository.DemoRepository
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.stereotype.Service

@Service
class DemoService(private val demoRepository: DemoRepository) {
    fun getDemo(id: Long): Demo {
        return demoRepository.findById(id).orElseThrow { IllegalArgumentException("해당 데이터가 없습니다.") }
    }

    fun createDemo(demo: Demo): Long {
        return demoRepository.save(demo).id
    }
}
