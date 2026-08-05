package com.wealthos.configuration

import com.wealthos.identity.application.CurrentUserResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain

@Configuration
class ApiSecurityConfiguration(
    private val currentUserResolver: CurrentUserResolver,
    @param:Value("\${wealthos.auth.fx-sync-client-id}")
    private val fxSyncClientId: String,
) {
    @Bean
    fun apiSecurity(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                ).permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/fx-rates/sync")
                    .access { authentication, _ ->
                        AuthorizationDecision(isApprovedFxSyncClient(authentication.get()))
                    }
                it.anyRequest()
                    .access { authentication, _ ->
                        AuthorizationDecision(isHumanAccessToken(authentication.get()))
                    }
            }
            .oauth2ResourceServer { it.jwt(withDefaults()) }
            .build()

    private fun isApprovedFxSyncClient(authentication: Authentication): Boolean =
        authentication is JwtAuthenticationToken &&
            authentication.token.subject == "$fxSyncClientId@clients" &&
            authentication.authorities.any { it.authority == FX_SYNC_AUTHORITY }

    private fun isHumanAccessToken(authentication: Authentication): Boolean {
        val subject = (authentication as? JwtAuthenticationToken)?.token?.subject ?: return false
        return !subject.endsWith(AUTH0_MACHINE_SUBJECT_SUFFIX) &&
            authentication.authorities.any { it.authority == WEALTH_ACCESS_AUTHORITY } &&
            currentUserResolver.resolve(authentication) != null
    }

    companion object {
        private const val WEALTH_ACCESS_AUTHORITY = "SCOPE_wealth:access"
        private const val FX_SYNC_AUTHORITY = "SCOPE_fx:sync"
        private const val AUTH0_MACHINE_SUBJECT_SUFFIX = "@clients"
    }
}
