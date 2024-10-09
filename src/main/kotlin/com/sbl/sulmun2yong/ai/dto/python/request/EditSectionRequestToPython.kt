package com.sbl.sulmun2yong.ai.dto.python.request

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSection
import java.util.UUID

data class EditSectionRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    val section: PythonFormattedSection,
) : EditRequestToPython
