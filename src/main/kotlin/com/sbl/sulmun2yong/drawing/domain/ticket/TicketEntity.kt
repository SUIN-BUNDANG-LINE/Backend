package com.sbl.sulmun2yong.drawing.domain.ticket

import jakarta.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
sealed class TicketEntity(
    @Column(nullable = false)
    open val isSelected: Boolean
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Entity
    @DiscriminatorValue("WINNING")
    data class Winning(
        val rewardName: String,
        val rewardCategory: String,
        override val isSelected: Boolean
    ) : TicketEntity(isSelected) {
        // JPA용 기본 생성자
        protected constructor() : this("", "", false)

        companion object {
            fun create(rewardName: String, rewardCategory: String): Winning {
                return Winning(
                    rewardName = rewardName,
                    rewardCategory = rewardCategory,
                    isSelected = false
                )
            }
        }
    }

    @Entity
    @DiscriminatorValue("NON_WINNING")
    data class NonWinning(
        override val isSelected: Boolean
    ) : TicketEntity(isSelected) {
        protected constructor() : this(false)

        companion object {
            fun create(): NonWinning {
                return NonWinning(isSelected = false)
            }
        }
    }
}
