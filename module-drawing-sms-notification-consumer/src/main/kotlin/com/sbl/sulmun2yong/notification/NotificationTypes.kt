package com.sbl.sulmun2yong.notification

// sms_notification_jobs.notification_type 값의 단일 진실 원천.
// 잡을 적재하는 도메인 리스너와 payload 파싱을 분기하는 SmsAttemptHandler 가 공유한다.
object NotificationTypes {
    const val DRAWING_SMS = "DRAWING_SMS"
    const val CO_FUNDING_FAILED_SMS = "CO_FUNDING_FAILED_SMS"
}
