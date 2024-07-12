package com.sbl.sulmun2yong.member.repository

import com.sbl.sulmun2yong.member.entity.Member
import org.springframework.data.mongodb.repository.MongoRepository

interface MemberRepository : MongoRepository<Member, String>
