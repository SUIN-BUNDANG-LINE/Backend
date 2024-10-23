package com.sbl.sulmun2yong.global.util

import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@ConditionalOnProperty(prefix = "fingerprint", name = ["mocking-server-url"], matchIfMissing = false)
@Component
class FakeFingerprintApi(
    @Value("\${fingerprint.mocking-server-url}")
    private val mockingServerUrl: String,
) {
    private val url = "$mockingServerUrl/finger-print"
    private val client: Client = ClientBuilder.newClient()

    fun callFingerPrintApi(visitorId: String) {
        val target = client.target(url)
        val invocationBuilder: Invocation.Builder = target.request(MediaType.APPLICATION_JSON_TYPE)
        val response: Response = invocationBuilder.get()

        if (response.status != Response.Status.OK.statusCode) {
            throw IllegalStateException("Fake 핑거프린트 API를 호출할 수 없습니다.")
        }
    }
}
