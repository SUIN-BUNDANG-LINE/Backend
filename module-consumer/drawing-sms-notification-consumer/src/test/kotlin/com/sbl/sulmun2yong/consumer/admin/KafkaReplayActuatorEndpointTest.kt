package com.sbl.sulmun2yong.consumer.admin

import com.sbl.sulmun2yong.global.kafka.consumer.replay.ReplayableConsumer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// KafkaReplayActuatorEndpoint 의 분기/계약을 고정한다.
// ReplayableConsumer 추상화에 위임하는지(DIP), 잘못된 입력 처리, target 라우팅을 검증한다.
class KafkaReplayActuatorEndpointTest {
    private fun mockConsumer(consumerName: String): ReplayableConsumer {
        val c = mock<ReplayableConsumer>()
        whenever(c.name).thenReturn(consumerName)
        return c
    }

    @Test
    fun `beginning 모드 - fromBeginning 호출 + dispatched 응답`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        val result =
            endpoint.replay(
                mode = "beginning",
                target = null,
                epochMillis = null,
                partition = null,
                offset = null,
            )

        verify(consumer).fromBeginning()
        assertEquals("dispatched", result["status"])
        assertEquals("beginning", result["mode"])
        assertEquals("drawing-notification-listener", result["target"])
    }

    @Test
    fun `timestamp 모드 + epochMillis - fromTimestamp 호출`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        val result =
            endpoint.replay(
                mode = "timestamp",
                target = null,
                epochMillis = 1_700_000_000_000L,
                partition = null,
                offset = null,
            )

        verify(consumer).fromTimestamp(1_700_000_000_000L)
        assertEquals("dispatched", result["status"])
        assertEquals(1_700_000_000_000L, result["epochMillis"])
    }

    @Test
    fun `timestamp 모드인데 epochMillis null - IllegalArgumentException`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        assertThrows<IllegalArgumentException> {
            endpoint.replay(
                mode = "timestamp",
                target = null,
                epochMillis = null,
                partition = null,
                offset = null,
            )
        }
    }

    @Test
    fun `offset 모드 + partition+offset - fromOffset 호출`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        val result =
            endpoint.replay(
                mode = "offset",
                target = null,
                epochMillis = null,
                partition = 2,
                offset = 100L,
            )

        verify(consumer).fromOffset(2, 100L)
        assertEquals("dispatched", result["status"])
        assertEquals(2, result["partition"])
        assertEquals(100L, result["offset"])
    }

    @Test
    fun `offset 모드인데 partition 또는 offset 누락 - IllegalArgumentException`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        assertThrows<IllegalArgumentException> {
            endpoint.replay(
                mode = "offset",
                target = null,
                epochMillis = null,
                partition = null,
                offset = 10L,
            )
        }
        assertThrows<IllegalArgumentException> {
            endpoint.replay(
                mode = "offset",
                target = null,
                epochMillis = null,
                partition = 1,
                offset = null,
            )
        }
    }

    @Test
    fun `알 수 없는 mode - error 응답`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        val result =
            endpoint.replay(
                mode = "unknown",
                target = null,
                epochMillis = null,
                partition = null,
                offset = null,
            )

        assertEquals("error", result["status"])
        val message = result["message"] as String
        assertTrue(message.contains("unknown mode"))
    }

    @Test
    fun `target 지정 시 해당 이름의 컨슈머에 위임`() {
        val a = mockConsumer("alpha")
        val b = mockConsumer("beta")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(a, b))

        val result =
            endpoint.replay(
                mode = "beginning",
                target = "beta",
                epochMillis = null,
                partition = null,
                offset = null,
            )

        verify(b).fromBeginning()
        assertEquals("dispatched", result["status"])
        assertEquals("beta", result["target"])
    }

    @Test
    fun `target null + 등록 컨슈머 2개 이상 - error 응답`() {
        val a = mockConsumer("alpha")
        val b = mockConsumer("beta")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(a, b))

        val result =
            endpoint.replay(
                mode = "beginning",
                target = null,
                epochMillis = null,
                partition = null,
                offset = null,
            )

        assertEquals("error", result["status"])
        val message = result["message"] as String
        assertTrue(message.contains("target 필수"))
    }

    @Test
    fun `unknown target - error 응답`() {
        val consumer = mockConsumer("drawing-notification-listener")
        val endpoint = KafkaReplayActuatorEndpoint(listOf(consumer))

        val result =
            endpoint.replay(
                mode = "beginning",
                target = "does-not-exist",
                epochMillis = null,
                partition = null,
                offset = null,
            )

        assertEquals("error", result["status"])
        val message = result["message"] as String
        assertTrue(message.contains("unknown target"))
    }
}
