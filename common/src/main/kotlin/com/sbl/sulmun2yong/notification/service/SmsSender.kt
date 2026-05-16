package com.sbl.sulmun2yong.notification.service

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent

interface SmsSender {
    fun sendWinnerNotification(event: DrawingCompletedEvent)
}
