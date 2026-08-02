package com.wealthos.adapter.persistence.liability

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "liabilities")
class LiabilityJpaEntity(
    @Id
    val id: UUID,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val archived: Boolean,
)
