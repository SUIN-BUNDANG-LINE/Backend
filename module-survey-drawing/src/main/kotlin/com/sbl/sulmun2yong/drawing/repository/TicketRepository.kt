package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.TicketEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TicketRepository : JpaRepository<TicketEntity, Long> {
    @Query("SELECT t FROM TicketEntity t WHERE t.drawingBoard.id = :boardId AND t.ticketIndex = :index")
    fun findByBoardAndIndex(
        @Param("boardId") boardId: UUID,
        @Param("index") index: Int,
    ): TicketEntity?

    // 선택됐는지 Compare And Swap
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE TicketEntity t SET t.isSelected = true
        WHERE t.drawingBoard.id = :boardId AND t.ticketIndex = :index AND t.isSelected = false
       """,
    )
    fun markSelectedCAS(
        @Param("boardId") boardId: UUID,
        @Param("index") index: Int,
    ): Int

    @Query(
        """
        SELECT COUNT(t) FROM TicketEntity t WHERE t.drawingBoard.id = :boardId AND t.isSelected = false
    """,
    )
    fun countRemaining(
        @Param("boardId") boardId: UUID,
    ): Long

}
