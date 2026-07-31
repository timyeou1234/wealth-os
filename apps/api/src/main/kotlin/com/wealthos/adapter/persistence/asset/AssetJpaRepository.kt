package com.wealthos.adapter.persistence.asset

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AssetJpaRepository : JpaRepository<AssetJpaEntity, UUID>
