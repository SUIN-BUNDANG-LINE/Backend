package com.sbl.sulmun2yong.rewarddrawing.repository

import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DrawingBoardRepository : MongoRepository<SurveyDocument, UUID> {
}
