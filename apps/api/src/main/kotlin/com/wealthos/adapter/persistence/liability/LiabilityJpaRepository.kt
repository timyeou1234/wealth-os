package com.wealthos.adapter.persistence.liability

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LiabilityJpaRepository : JpaRepository<LiabilityJpaEntity, UUID>
