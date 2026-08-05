package com.wealthos.identity.domain

data class User(
    val id: UserId,
    val externalIdentity: ExternalIdentity,
    val email: String,
)
