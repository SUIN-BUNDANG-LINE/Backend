package com.sbl.sulmun2yong.ai.repository

import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ChatRepository(
    private val requestToPythonServerTemplate: RestTemplate,
)
