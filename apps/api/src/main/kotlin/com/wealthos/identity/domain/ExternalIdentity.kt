package com.wealthos.identity.domain

data class ExternalIdentity(
    val issuer: String,
    val subject: String,
) {
    init {
        require(issuer.isNotBlank()) { "issuer must not be blank" }
        require(subject.isNotBlank()) { "subject must not be blank" }
    }
}
