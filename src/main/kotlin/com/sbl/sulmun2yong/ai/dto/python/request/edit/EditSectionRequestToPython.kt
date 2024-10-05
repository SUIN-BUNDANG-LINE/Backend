package com.sbl.sulmun2yong.ai.dto.python.request.edit

import com.sbl.sulmun2yong.ai.entity.PythonServerSectionFormat
import java.util.UUID

class EditSectionRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    val section: PythonServerSectionFormat,
) : EditRequestToPython
