package com.sbl.sulmun2yong.global.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.util.Date

abstract class BaseTimeDocument {
    @CreatedDate
    var createdAt: Date = Date()

    @LastModifiedDate
    var updatedAt: Date = Date()
}
