package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.entity.AIGenerateLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AIGenerateLogRepository : JpaRepository<AIGenerateLog, UUID>
