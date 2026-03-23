package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.entity.ParticipantEntity
import com.sbl.sulmun2yong.survey.entity.ResponseEntity
import com.sbl.sulmun2yong.survey.entity.SurveyEntity
import com.sbl.sulmun2yong.user.entity.UserEntity
import com.sbl.sulmun2yong.user.util.dummy.Probability
import com.sbl.sulmun2yong.user.util.dummy.RandomParticipantGenerateUtil.generateRandomParticipants
import com.sbl.sulmun2yong.user.util.dummy.RandomResponseGenerateUtil.generateSurveyResponseEntities
import com.sbl.sulmun2yong.user.util.dummy.RandomSurveyGenerateUtil.generateRandomSurvey
import com.sbl.sulmun2yong.user.util.dummy.RandomUserGenerateUtil.generateRandomUser
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
        val userCount = max(1, surveyCount / 10)
        val users = (1..userCount).map { generateRandomUser() }
        val surveys = (1..surveyCount).map { generateRandomSurvey(users.random().id) }

        users.forEach { entityManager.persist(UserEntity.of(it)) }
        surveys.forEach { entityManager.persist(getRandomSurveyEntity(it)) }

        for (survey in surveys) {
            val participants = generateRandomParticipants(survey.id)
            val responseEntities: List<ResponseEntity> = generateSurveyResponseEntities(survey, participants)
            participants.forEach { entityManager.persist(ParticipantEntity.of(it)) }
            responseEntities.forEach { entityManager.persist(it) }
        }
    }

    private fun getRandomSurveyEntity(survey: Survey): SurveyEntity {
        val entity = SurveyEntity.from(survey)
        // DummyData에서 isDeleted 상태를 랜덤으로 설정하는 로직은
        // SurveyEntity가 data class가 아니므로 copy 불가 → 별도 처리 필요 시 추후 대응
        return entity
    }
}
