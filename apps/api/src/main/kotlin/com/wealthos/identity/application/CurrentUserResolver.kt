package com.wealthos.identity.application

import com.wealthos.identity.domain.ExternalIdentity
import com.wealthos.identity.domain.User
import com.wealthos.identity.domain.UserId
import com.wealthos.identity.domain.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.UUID

@Service
class CurrentUserResolver(
    private val users: UserRepository,
    @Value("\${wealthos.auth.allowed-emails}") allowedEmails: String,
) {
    private val allowlist =
        allowedEmails
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { it.lowercase(Locale.ROOT) }
            .toSet()

    fun resolve(authentication: Authentication?): UserId? {
        val token = (authentication as? JwtAuthenticationToken)?.token ?: return null
        val issuer = token.issuer?.toString() ?: return null
        val subject = token.subject?.takeIf(String::isNotBlank) ?: return null
        val email = token.getClaimAsString("email")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val verified = token.getClaim<Boolean>("email_verified") == true
        if (!verified || email.lowercase(Locale.ROOT) !in allowlist) return null

        val identity = ExternalIdentity(issuer, subject)
        return users.findByExternalIdentity(identity)?.id ?: provision(identity, email)
    }

    private fun provision(
        identity: ExternalIdentity,
        email: String,
    ): UserId =
        try {
            users.save(User(UserId(UUID.randomUUID()), identity, email)).id
        } catch (_: DataIntegrityViolationException) {
            users.findByExternalIdentity(identity)?.id ?: throw IllegalStateException("User provisioning failed")
        }
}
