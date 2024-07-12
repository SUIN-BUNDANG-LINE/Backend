package com.sbl.sulmun2yong.member.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "member")
data class Member(
    @Id
    val id: String = "",
    val userKey: String = "",
    val provider: String = "",
    val authorityLevel: String = "",
    val phoneNumber: String = "",
)
