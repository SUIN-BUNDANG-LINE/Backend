package com.sbl.sulmun2yong.demo.repository

import com.sbl.sulmun2yong.demo.entity.Demo
import org.springframework.data.jpa.repository.JpaRepository

interface DemoRepository : JpaRepository<Demo, Long>
