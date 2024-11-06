package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.entity.AIGenerateLogDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
interface AIGenerateLogRepository : MongoRepository<AIGenerateLogDocument, UUID>
