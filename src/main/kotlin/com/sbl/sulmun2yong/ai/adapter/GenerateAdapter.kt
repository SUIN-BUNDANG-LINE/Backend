package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithFileUrlRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithTextDocumentRequestToPython
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
        val generateSurveyResponseFromPython =
            generateRepository.requestWithFileUrl(
                GenerateWithFileUrlRequestToPython(
                    job = job,
                    groupName = groupName,
                    userPrompt = userPrompt,
                    fileUrl = fileUrl,
                ),
            )

        return generateSurveyResponseFromPython.toDomain()
    }

    fun requestSurveyGenerationWithTextDocument(
        job: String,
        groupName: String,
        textDocument: String,
        userPrompt: String,
    ): AIGeneratedSurvey {
        val generateSurveyResponseFromPython =
            generateRepository.requestWithTextDocument(
                GenerateWithTextDocumentRequestToPython(
                    job = job,
                    groupName = groupName,
                    userPrompt = userPrompt,
                    textDocument = textDocument,
                ),
            )

        return generateSurveyResponseFromPython.toDomain()
    }
}
