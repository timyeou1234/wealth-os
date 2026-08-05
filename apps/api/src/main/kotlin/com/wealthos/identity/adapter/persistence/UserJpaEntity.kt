package com.wealthos.identity.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "users_external_identity_unique", columnNames = ["issuer", "subject"])],
)
class UserJpaEntity(
    @Id
    val id: UUID,
    @Column(nullable = false, length = 512)
    val issuer: String,
    @Column(nullable = false)
    val subject: String,
    @Column(nullable = false)
    val email: String,
)
