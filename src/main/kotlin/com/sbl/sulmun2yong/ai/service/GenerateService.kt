package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.GenerateAdapter
import com.sbl.sulmun2yong.global.util.validator.FileUrlValidator
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import org.springframework.stereotype.Service

@Service
class GenerateService(
    private val fileUrlValidator: FileUrlValidator,
    private val generateAdapter: GenerateAdapter,
) {
    fun generateSurvey(
        job: String,
        groupName: String,
        fileUrl: String,
    ): SurveyMakeInfoResponse {
        val allowedExtensions = mutableListOf(".txt", ".pdf")
        fileUrlValidator.validateFileUrlOf(fileUrl, allowedExtensions)

        return generateAdapter.requestSurveyGenerationWithFileUrl(job, groupName, fileUrl)
    }
}
