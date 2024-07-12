package com.sbl.sulmun2yong.member.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "member")
data class MemberDocuments(
    @Id
    val id: String = "",
    val userKey: String = "",
    val provider: String = "",
    val authorityLevel: String = "",
) : BaseTimeDocument()
