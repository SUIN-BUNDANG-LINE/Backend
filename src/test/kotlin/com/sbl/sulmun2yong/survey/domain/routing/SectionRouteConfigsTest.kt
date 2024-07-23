package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.exception.InvalidSectionRouteConfigsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class SectionRouteConfigsTest {
    @Test
    fun `SectionRouteConfigs는 비어있으면 안된다`() {
        assertThrows<InvalidSectionRouteConfigsException> { SectionRouteConfigs(listOf()) }
    }

    @Test
    fun `SectionRouteConfigs에는 중복되는 content가 있으면 안된다`() {
        assertThrows<InvalidSectionRouteConfigsException> {
            SectionRouteConfigs(
                listOf(
                    SectionRouteConfig("a", UUID.randomUUID()),
                    SectionRouteConfig("b", UUID.randomUUID()),
                    SectionRouteConfig(null, UUID.randomUUID()),
                    SectionRouteConfig(null, UUID.randomUUID()),
                ),
            )
        }

        assertThrows<InvalidSectionRouteConfigsException> {
            SectionRouteConfigs(
                listOf(
                    SectionRouteConfig("a", UUID.randomUUID()),
                    SectionRouteConfig("a", UUID.randomUUID()),
                    SectionRouteConfig("b", UUID.randomUUID()),
                ),
            )
        }
    }
}
