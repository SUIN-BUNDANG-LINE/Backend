package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.result.QuestionFilter
import com.sbl.sulmun2yong.survey.domain.result.QuestionResult
import com.sbl.sulmun2yong.survey.domain.result.ResultDetails
import com.sbl.sulmun2yong.survey.domain.result.SurveyResult
import java.util.UUID

object SurveyResultConstFactory {
    val JOB_QUESTION_ID = UUID.randomUUID()
    val JOB_QUESTION_CONTENTS = listOf("학생", "직장인", "자영업자", "무직")

    val GENDER_QUESTION_ID = UUID.randomUUID()
    val GENDER_QUESTION_CONTENTS = listOf("남자", "여자")

    val FOOD_MULTIPLE_CHOICE_QUESTION_ID = UUID.randomUUID()
    val FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS = listOf("한식", "일식", "패스트푸드", "중식", "분식", "양식")

    val FOOD_TEXT_RESPONSE_QUESTION_ID = UUID.randomUUID()
    val FOOD_TEXT_RESPONSE_QUESTION_CONTENTS = listOf("맛있어요!", "매콤해요!", "달아요!", "짭짤해요!")

    val PARTICIPANT_ID_1 = UUID.randomUUID()

    /** 1번 참가자의 응답(학생, 남자, 한식 & 일식, 맛있어요!) */
    val PARTICIPANT_RESULT_DETAILS_1 =
        listOf(
            ResultDetails(
                participantId = PARTICIPANT_ID_1,
                contents = listOf(JOB_QUESTION_CONTENTS[0]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_1,
                contents = listOf(GENDER_QUESTION_CONTENTS[0]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_1,
                contents = listOf(FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[0], FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[1]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_1,
                contents = listOf(FOOD_TEXT_RESPONSE_QUESTION_CONTENTS[0]),
            ),
        )

    val PARTICIPANT_ID_2 = UUID.randomUUID()

    /** 2번 참가자의 응답(직장인, 여자, 한식 & 패스트푸드 & 중식, 매콥해요!) */
    val PARTICIPANT_RESULT_DETAILS_2 =
        listOf(
            ResultDetails(
                participantId = PARTICIPANT_ID_2,
                contents = listOf(JOB_QUESTION_CONTENTS[1]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_2,
                contents = listOf(GENDER_QUESTION_CONTENTS[1]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_2,
                contents =
                    listOf(
                        FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[0],
                        FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[2],
                        FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[3],
                    ),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_2,
                contents = listOf(FOOD_TEXT_RESPONSE_QUESTION_CONTENTS[1]),
            ),
        )

    val PARTICIPANT_ID_3 = UUID.randomUUID()

    /** 3번 참가자의 응답(학생, 여자, 패스트푸드 & 분식, 달아요!) */
    val PARTICIPANT_RESULT_DETAILS_3 =
        listOf(
            ResultDetails(
                participantId = PARTICIPANT_ID_3,
                contents = listOf(JOB_QUESTION_CONTENTS[0]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_3,
                contents = listOf(GENDER_QUESTION_CONTENTS[1]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_3,
                contents = listOf(FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[2], FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[4]),
            ),
            ResultDetails(
                participantId = PARTICIPANT_ID_3,
                contents = listOf(FOOD_TEXT_RESPONSE_QUESTION_CONTENTS[2]),
            ),
        )

    val QUESTION_RESULT_1 =
        QuestionResult(
            questionId = JOB_QUESTION_ID,
            resultDetails = listOf(PARTICIPANT_RESULT_DETAILS_1[0], PARTICIPANT_RESULT_DETAILS_2[0], PARTICIPANT_RESULT_DETAILS_3[0]),
            contents = JOB_QUESTION_CONTENTS.toSortedSet(),
        )

    val QUESTION_RESULT_2 =
        QuestionResult(
            questionId = GENDER_QUESTION_ID,
            resultDetails = listOf(PARTICIPANT_RESULT_DETAILS_1[1], PARTICIPANT_RESULT_DETAILS_2[1], PARTICIPANT_RESULT_DETAILS_3[1]),
            contents = GENDER_QUESTION_CONTENTS.toSortedSet(),
        )

    val QUESTION_RESULT_3 =
        QuestionResult(
            questionId = FOOD_MULTIPLE_CHOICE_QUESTION_ID,
            resultDetails = listOf(PARTICIPANT_RESULT_DETAILS_1[2], PARTICIPANT_RESULT_DETAILS_2[2], PARTICIPANT_RESULT_DETAILS_3[2]),
            contents = FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS.toSortedSet(),
        )

    val QUESTION_RESULT_4 =
        QuestionResult(
            questionId = FOOD_TEXT_RESPONSE_QUESTION_ID,
            resultDetails = listOf(PARTICIPANT_RESULT_DETAILS_1[3], PARTICIPANT_RESULT_DETAILS_2[3], PARTICIPANT_RESULT_DETAILS_3[3]),
            contents = FOOD_TEXT_RESPONSE_QUESTION_CONTENTS.toSortedSet(),
        )

    val SURVEY_RESULT = SurveyResult(questionResults = listOf(QUESTION_RESULT_1, QUESTION_RESULT_2, QUESTION_RESULT_3, QUESTION_RESULT_4))

    val EXCEPT_STUDENT_FILTER = QuestionFilter(JOB_QUESTION_ID, listOf(JOB_QUESTION_CONTENTS[0]), false)
    val MAN_FILTER = QuestionFilter(GENDER_QUESTION_ID, listOf(GENDER_QUESTION_CONTENTS[0]), true)
    val K_J_FOOD_FILTER =
        QuestionFilter(
            FOOD_MULTIPLE_CHOICE_QUESTION_ID,
            listOf(FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[0], FOOD_MULTIPLE_CHOICE_QUESTION_CONTENTS[1]),
            true,
        )
}
