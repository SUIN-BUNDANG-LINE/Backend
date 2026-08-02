package com.sbl.sulmun2yong.notification.service

import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedEvent
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.notification.exception.SmsSendException

interface SmsSender {
    /**
     * 당첨자에게 SMS 발송. 발송 실패는 [SmsSendException] 으로 통일해야 재시도/DLT 회로에 진입한다.
     * 시스템 오류 (JSON 파싱, OOM 등) 는 [SmsSendException] 외 타입으로 전파해야 한다 — 그래야 catch 회로가 그것들을 무한 재시도하지 않는다.
     */
    @Throws(SmsSendException::class)
    fun sendWinnerNotification(event: DrawingCompletedEvent)

    /** 모금 무산 안내 SMS 발송. 실패 계약은 [sendWinnerNotification] 과 동일하다. */
    @Throws(SmsSendException::class)
    fun sendCoFundingFailedNotification(event: CoFundingFailedEvent)
}
