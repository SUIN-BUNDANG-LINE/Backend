package com.sbl.sulmun2yong.notification.handler

import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity

interface DltDispatcher {
    fun dispatch(job: SmsNotificationJobEntity)
}
