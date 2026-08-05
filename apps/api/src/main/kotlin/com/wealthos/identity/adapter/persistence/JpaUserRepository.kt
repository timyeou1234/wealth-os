package com.wealthos.identity.adapter.persistence

import com.wealthos.identity.domain.ExternalIdentity
import com.wealthos.identity.domain.User
import com.wealthos.identity.domain.UserId
import com.wealthos.identity.domain.UserRepository
import org.springframework.stereotype.Repository

@Repository
class JpaUserRepository(
    private val repository: UserJpaRepository,
) : UserRepository {
    override fun findByExternalIdentity(identity: ExternalIdentity): User? =
        repository.findByIssuerAndSubject(identity.issuer, identity.subject)?.toDomain()

    override fun save(user: User): User = repository.save(user.toEntity()).toDomain()

    private fun User.toEntity(): UserJpaEntity =
        UserJpaEntity(
            id = id.value,
            issuer = externalIdentity.issuer,
            subject = externalIdentity.subject,
            email = email,
        )

    private fun UserJpaEntity.toDomain(): User =
        User(
            id = UserId(id),
            externalIdentity = ExternalIdentity(issuer, subject),
            email = email,
        )
}
