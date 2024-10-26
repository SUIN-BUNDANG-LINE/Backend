package com.sbl.sulmun2yong.ai.repository.redis

import com.sbl.sulmun2yong.ai.entity.redis.AIDemoCountRedisEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AIDemoCountRedisRepository : CrudRepository<AIDemoCountRedisEntity, String>
