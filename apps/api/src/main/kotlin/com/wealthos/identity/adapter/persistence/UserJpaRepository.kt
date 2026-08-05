package com.wealthos.identity.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserJpaRepository : JpaRepository<UserJpaEntity, UUID> {
    fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserJpaEntity?
}
