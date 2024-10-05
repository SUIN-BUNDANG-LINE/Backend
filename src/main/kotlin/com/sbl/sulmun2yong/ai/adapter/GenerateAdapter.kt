package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.ai.repository.GenerateRepository
import org.springframework.stereotype.Component

@Component
class GenerateAdapter(
    private val generateRepository: GenerateRepository,
) {
    fun requestSurveyGenerationWithFileUrl(
        job: String,
        groupName: String,
        fileUrl: String,
        userPrompt: String,
    ): AIGeneratedSurvey {
        val aiSurveyGenerationResponseFromPython =
            generateRepository.requestWithFileUrl(
                job = job,
                groupName = groupName,
                fileUrl = fileUrl,
                userPrompt = userPrompt,
            )

        return aiSurveyGenerationResponseFromPython.toDomain()
    }

    fun requestSurveyGenerationWithTextDocument(
        job: String,
        groupName: String,
        textDocument: String,
        userPrompt: String,
    ): AIGeneratedSurvey {
        val aiSurveyGenerationResponseFromPython =
            generateRepository.requestWithTextDocument(
                job = job,
                groupName = groupName,
                textDocument = textDocument,
                userPrompt = userPrompt,
            )

        return aiSurveyGenerationResponseFromPython.toDomain()
    }
}
