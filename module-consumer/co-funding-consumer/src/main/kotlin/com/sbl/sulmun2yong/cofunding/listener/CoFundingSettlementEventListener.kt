package com.sbl.sulmun2yong.cofunding.listener

import com.sbl.sulmun2yong.cofunding.dto.event.CoPaymentSettledConsumedEvent
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

// 장벽 집계 - settled 를 받을 때마다 전원 완료 CAS 를 시도한다.
// 어댑터의 트랜잭션 안에서 동기 실행되므로 tryConfirm 과 tryActivate 가 원자적이다 :
// 활성화 전에 죽으면 CONFIRMED 도 롤백되고, ack 미발행이라 재전달이 다시 데려온다.
@Component
class CoFundingSettlementEventListener(
    private val coFundingRepository: CoFundingRepository,
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingSettlementEventListener::class.java)
    }

    @EventListener
    fun handle(event: CoPaymentSettledConsumedEvent) {
        val now = LocalDateTime.now()
        // 1 = 마지막 결제를 확인해 FUNDING -> CONFIRMED 를 만든 유일한 승자.
        // 0 = 아직 미결제 잔여 / 이미 종료(재수신/패배) - no-op 이 곧 멱등이다.
        if (coFundingRepository.tryConfirm(UUID.fromString(event.fundingId), now) != 1) return

        if (surveyRepository.tryActivate(UUID.fromString(event.surveyId), now) == 1) {
            log.info(
                "공동 모금 개설 확정 - 설문 활성화: fundingId={}, surveyId={}",
                event.fundingId,
                event.surveyId,
            )
        } else {
            // 승자인데 설문이 결제 대기가 아님 - 정상 흐름에선 도달 불가, 데이터 점검 필요
            log.warn("개설 확정했으나 설문 활성화 no-op: surveyId={}", event.surveyId)
        }
    }
}
