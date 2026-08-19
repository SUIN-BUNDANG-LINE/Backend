package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.survey.entity.ResponseEntity
import com.sbl.sulmun2yong.user.util.dummy.Probability
import com.sbl.sulmun2yong.user.util.dummy.RandomParticipantGenerateUtil.generateRandomParticipants
import com.sbl.sulmun2yong.user.util.dummy.RandomResponseGenerateUtil.generateSurveyResponseEntities
import com.sbl.sulmun2yong.user.util.dummy.RandomSurveyGenerateUtil.generateRandomSurvey
import com.sbl.sulmun2yong.user.util.dummy.RandomUserGenerateUtil.generateRandomMakerId
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

@Service
class DummyDataService(
    @PersistenceContext
    private val entityManager: EntityManager,
) {
    companion object {
        private val randomIsDeleted = Probability(0.1)
    }

    @Transactional
    fun insertDummyData(surveyCount: Int) {
        val makerCount = max(1, surveyCount / 10)
        val makerIds = (1..makerCount).map { generateRandomMakerId() }
        val surveys = (1..surveyCount).map { generateRandomSurvey(makerIds.random()) }

        surveys.forEach { entityManager.persist(it) }

        for (survey in surveys) {
            val participants = generateRandomParticipants(survey.id)
            val responseEntities: List<ResponseEntity> = generateSurveyResponseEntities(survey, participants)
            participants.forEach { entityManager.persist(it) }
            responseEntities.forEach { entityManager.persist(it) }
        }
    }
}
