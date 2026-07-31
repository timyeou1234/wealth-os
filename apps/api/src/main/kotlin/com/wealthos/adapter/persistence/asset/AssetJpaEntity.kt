package com.wealthos.adapter.persistence.asset

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "assets")
class AssetJpaEntity(
    @Id
    val id: UUID,
    @Column(nullable = false)
    val name: String,
    @Column(name = "asset_type", nullable = false)
    val assetType: String,
    @Column(nullable = false)
    val liquidity: String,
)
