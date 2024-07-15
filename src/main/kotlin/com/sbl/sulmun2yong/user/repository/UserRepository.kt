package com.sbl.sulmun2yong.user.repository

import com.sbl.sulmun2yong.user.entity.UserDocument
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : MongoRepository<UserDocument, UUID> {
    fun findByProviderAndProviderId(
        provider: String,
        providerId: String,
    ): Optional<UserDocument>
}
