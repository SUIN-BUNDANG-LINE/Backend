package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.entity.ParticipantDocument
import com.sbl.sulmun2yong.survey.entity.ResponseDocument
import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import com.sbl.sulmun2yong.user.entity.UserDocument
import com.sbl.sulmun2yong.user.util.dummy.Probability
import com.sbl.sulmun2yong.user.util.dummy.RandomParticipantGenerateUtil.generateRandomParticipants
import com.sbl.sulmun2yong.user.util.dummy.RandomResponseGenerateUtil.generateSurveyResponseDocuments
import com.sbl.sulmun2yong.user.util.dummy.RandomSurveyGenerateUtil.generateRandomSurvey
import com.sbl.sulmun2yong.user.util.dummy.RandomUserGenerateUtil.generateRandomUser
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import kotlin.math.max

@Service
class DummyDataService(
    private val mongoTemplate: MongoTemplate,
) {
    companion object {
        private val randomIsDeleted = Probability(0.1)
    }

    fun insertDummyData(surveyCount: Int) {
        val userCount = max(1, surveyCount / 10)
        val users = (1..userCount).map { generateRandomUser() }
        val surveys = (1..surveyCount).map { generateRandomSurvey(users.random().id) }

        mongoTemplate.insert(users.map { UserDocument.of(it) }, UserDocument::class.java)
        mongoTemplate.insert(surveys.map { getRandomSurveyDocument(it) }, SurveyDocument::class.java)

        for (survey in surveys) {
            val participants = generateRandomParticipants(survey.id)
            val responseDocuments: List<ResponseDocument> = generateSurveyResponseDocuments(survey, participants)
            mongoTemplate.insert(participants.map { ParticipantDocument.of(it) }, ParticipantDocument::class.java)
            mongoTemplate.insert(responseDocuments, ResponseDocument::class.java)
        }
    }

    private fun getRandomSurveyDocument(survey: Survey): SurveyDocument {
        val surveyDocument = SurveyDocument.from(survey)
        if (randomIsDeleted.isWinning()) return surveyDocument.copy(isDeleted = true)
        return surveyDocument
    }
}
