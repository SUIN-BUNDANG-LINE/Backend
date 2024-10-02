package com.sbl.sulmun2yong.global.fingerprint

import com.fingerprint.api.FingerprintApi
import com.fingerprint.model.BotdDetectionResult
import com.fingerprint.model.EventResponse
import com.fingerprint.model.ProductsResponse
import com.fingerprint.model.Response
import com.fingerprint.model.ResponseVisits
import com.fingerprint.sdk.ApiClient
import com.fingerprint.sdk.Configuration
import com.fingerprint.sdk.Region
import com.sbl.sulmun2yong.global.util.exception.UncleanVisitorException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class FingerprintApi(
    @Value("\${fingerprint.secret-key}")
    private val secretKey: String,
) {
    private val client: ApiClient =
        Configuration.getDefaultApiClient(
            secretKey,
            Region.ASIA,
        )
    val api = FingerprintApi(client)

    fun validateVisitorId(visitorId: String) {
        val visits = getVisits(visitorId)
        if (visits.isNullOrEmpty()) {
            throw Exception("Invalid visitorId")
        }
        checkIsVisitorClean(visits)
    }

    private fun getVisits(visitorId: String): MutableList<ResponseVisits>? {
        val response: Response = api.getVisits(visitorId, null, null, 1, null, null)
        return response.visits
    }

    private fun getEvent(requestId: String): ProductsResponse {
        val response: EventResponse = api.getEvent(requestId)
        return response.products
    }

    private fun checkIsVisitorClean(visits: MutableList<ResponseVisits>) {
        val product = getEvent(visits[0].requestId)
        if (product.tampering.data.result == true ||
            product.botd.data.bot.result !== BotdDetectionResult.ResultEnum.NOT_DETECTED
        ) {
            throw UncleanVisitorException()
        }
    }
}
