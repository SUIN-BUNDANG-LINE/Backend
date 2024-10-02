package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.global.error.BusinessException
import com.sbl.sulmun2yong.global.error.ErrorCode

class InvalidSurveyEditException : BusinessException(ErrorCode.INVALID_SURVEY_EDIT)
