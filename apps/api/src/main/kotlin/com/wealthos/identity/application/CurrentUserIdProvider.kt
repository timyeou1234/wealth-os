package com.wealthos.identity.application

import com.wealthos.identity.domain.UserId
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentUserIdProvider(
    private val resolver: CurrentUserResolver,
) {
    fun get(): UserId {
        val authentication = SecurityContextHolder.getContext().authentication
        return resolver.resolve(authentication) ?: throw AccessDeniedException("Authenticated user is not allowed")
    }
}
