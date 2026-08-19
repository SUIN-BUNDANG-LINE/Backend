package com.sbl.sulmun2yong.global.test

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingBoardStatus
import com.sbl.sulmun2yong.drawing.repository.DrawingBoardRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 부하 테스트(k6)용 설문 활성화 엔드포인트.
 *
 * 경품 설문이 결제 대기(PENDING_PAYMENT 보드)에 머물러 응답을 못 받는 것을 우회하기 위해
 * 보드를 활성화하고 설문을 곧바로 IN_PROGRESS 로 여는 엔드포인트를 제공한다(외부 PG 없이 부하 테스트용).
 *
 * JWT 발급은 auth-service 의 TestAuthController 로 분리되었고, 여기는 설문 도메인 접근만 담당한다.
 * 프로덕션에서 노출되지 않도록 `test-auth.enabled=true` 일 때만 빈이 등록된다.
 */
@RestController
@RequestMapping("/api/v1/test")
@ConditionalOnProperty(prefix = "test-auth", name = ["enabled"], havingValue = "true")
class TestSurveyController(
    private val surveyRepository: SurveyRepository,
    private val drawingBoardRepository: DrawingBoardRepository,
) {
    // 결제 우회 — 결제 대기 보드가 있으면 활성화하고, 설문을 결제 없이 IN_PROGRESS 로 전환해 응답을 받게 한다.
    // start() 가 lazy 컬렉션(rewardEntities)을 건드리므로 세션 유지를 위해 트랜잭션 안에서 실행한다.
    @Transactional
    @PostMapping("/surveys/{surveyId}/activate")
    fun activateSurvey(
        @PathVariable surveyId: UUID,
    ): ResponseEntity<Map<String, String>> {
        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalse(surveyId)
                .orElseThrow { IllegalArgumentException("설문을 찾을 수 없습니다: $surveyId") }
        // 보드 생성은 접수 사가의 판정 리스너 몫인데 부하 테스트는 사가를 우회하므로,
        // 경품 설문이면 리스너와 동형으로(findBySurveyId 멱등) 없으면 만들고 곧장 활성화한다.
        val rewardSetting = survey.rewardSetting
        if (rewardSetting is ImmediateDrawSetting) {
            val board =
                drawingBoardRepository.findBySurveyId(surveyId).orElseGet {
                    drawingBoardRepository.save(
                        DrawingBoard.create(
                            surveyId = surveyId,
                            boardSize = rewardSetting.targetParticipantCount,
                            rewards = rewardSetting.rewards,
                        ),
                    )
                }
            if (board.status == DrawingBoardStatus.PENDING_PAYMENT) board.activate()
        }
        val activated = if (survey.status == SurveyStatus.IN_PROGRESS) survey else survey.start()
        surveyRepository.save(activated)
        return ResponseEntity.ok(mapOf("surveyId" to surveyId.toString(), "status" to activated.status.name))
    }
}
