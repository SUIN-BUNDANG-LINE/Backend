package com.sbl.sulmun2yong.global.response

import org.springframework.http.HttpStatus

enum class SuccessCode(val code: String, val message: String, val httpStatus: HttpStatus) {
    // Demo (DM)
    CREATE_DEMO_SUCCESS("DM0001", "데모 정보 생성을 성공했습니다.", HttpStatus.CREATED),
    FIND_DEMO_SUCCESS("DM0002", "데모 정보 조회를 성공했습니다.", HttpStatus.OK),
}
