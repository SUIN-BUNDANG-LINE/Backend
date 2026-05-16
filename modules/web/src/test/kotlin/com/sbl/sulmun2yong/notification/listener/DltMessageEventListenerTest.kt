package com.sbl.sulmun2yong.notification.listener

import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant

class DltMessageEventListenerTest {
    private val dltMessageRepository: DltMessageRepository = mock()
    private val listener = DltMessageEventListener(dltMessageRepository)

    @Test
    fun `DLT 이벤트를 받으면 모든 필드가 매핑되어 영속화된다`() {
        val failedAt = Instant.now()
        val event =
            DltSmsNotificationEvent(
                eventId = "evt-1",
                payload = """{"foo":"bar"}""",
                notificationType = "DRAWING_SMS",
                retryCount = 5,
                lastError = "timeout",
                failedAt = failedAt,
            )

        listener.handle(DltSmsNotificationConsumedEvent(event))

        verify(dltMessageRepository).save(
            argThat<DltMessageEntity> {
                eventId == "evt-1" &&
                    notificationType == "DRAWING_SMS" &&
                    retryCount == 5 &&
                    lastError == "timeout" &&
                    this.failedAt == failedAt
            },
        )
    }
}
