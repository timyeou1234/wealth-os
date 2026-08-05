package com.wealthos.identity.domain

interface UserRepository {
    fun findByExternalIdentity(identity: ExternalIdentity): User?

    fun save(user: User): User
}
