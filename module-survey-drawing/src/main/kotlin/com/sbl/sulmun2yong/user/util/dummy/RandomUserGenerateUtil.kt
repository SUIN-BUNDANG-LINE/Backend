package com.sbl.sulmun2yong.user.util.dummy

import java.util.UUID

// 더미 설문의 제작자 ID 만 만든다 - users 는 auth 서비스 소유(다른 DB)라 여기서 쓰지 않는다.
// surveys.maker_id 에 외래키가 없어 실존하지 않는 ID 여도 무방하며,
// 그 설문의 제작자 프로필만 조회되지 않는다(더미 데이터의 허용 범위).
object RandomUserGenerateUtil {
    fun generateRandomMakerId(): UUID = UUID.randomUUID()
}
