package com.sbl.sulmun2yong.survey.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "section_route_configs")
class SectionRouteConfigEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    val section: SectionEntity,
    @Column(nullable = false)
    val orderIndex: Int,
    val choiceContent: String?,
    @Column(columnDefinition = "BINARY(16)")
    val nextSectionId: UUID?,
)
