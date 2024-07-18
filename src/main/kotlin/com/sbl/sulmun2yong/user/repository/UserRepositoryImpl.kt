package com.sbl.sulmun2yong.user.repository

import com.sbl.sulmun2yong.user.entity.UserDocument
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val mongoTemplate: MongoTemplate,
) : UserRepositoryCustom {
    override fun countByNicknameRegex(nicknameRegex: String): Long {
        val query = Query()
        query.addCriteria(Criteria.where("nickname").regex(nicknameRegex))
        return mongoTemplate.count(query, UserDocument::class.java)
    }
}
