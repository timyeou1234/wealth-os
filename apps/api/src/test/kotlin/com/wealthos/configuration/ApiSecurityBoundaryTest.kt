package com.wealthos.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.wealthos.identity.adapter.persistence.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:api-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "wealthos.auth.allowed-emails=allowed@example.com,second@example.com",
        "wealthos.auth.fx-sync-client-id=approved-operations-client",
    ],
)
@AutoConfigureMockMvc
class ApiSecurityBoundaryTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @Autowired
    private lateinit var users: UserJpaRepository

    @Test
    fun `personal financial API rejects an unauthenticated request`() {
        mockMvc.get("/api/v1/assets")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `authenticated user can invoke a personal financial endpoint`() {
        mockMvc.get("/api/v1/assets") {
            with(
                userJwt("google-oauth2|person", "allowed@example.com"),
            )
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `human access token without product scope cannot invoke a personal financial endpoint`() {
        mockMvc.get("/api/v1/assets") {
            with(
                jwt().jwt {
                    it.issuer("https://wealth-os-test.example/")
                        .subject("google-oauth2|person-without-scope")
                        .claim("email", "allowed@example.com")
                        .claim("email_verified", true)
                },
            )
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `non-allowlisted user cannot invoke a personal financial endpoint`() {
        mockMvc.get("/api/v1/assets") {
            with(
                jwt().jwt {
                    it.issuer("https://wealth-os-test.example/")
                        .subject("google-oauth2|stranger")
                        .claim("email", "stranger@example.com")
                        .claim("email_verified", true)
                },
            )
        }.andExpect { status { isForbidden() } }

        assertThat(
            users.findByIssuerAndSubject("https://wealth-os-test.example/", "google-oauth2|stranger"),
        ).isNull()
    }

    @Test
    fun `unverified user cannot invoke a personal financial endpoint`() {
        mockMvc.get("/api/v1/assets") {
            with(
                jwt().jwt {
                    it.issuer("https://wealth-os-test.example/")
                        .subject("google-oauth2|unverified")
                        .claim("email", "allowed@example.com")
                        .claim("email_verified", false)
                },
            )
        }.andExpect { status { isForbidden() } }

        assertThat(
            users.findByIssuerAndSubject("https://wealth-os-test.example/", "google-oauth2|unverified"),
        ).isNull()
    }

    @Test
    fun `human access token cannot invoke an operational endpoint`() {
        mockMvc.post("/api/v1/fx-rates/sync") {
            with(jwt().jwt { it.subject("google-oauth2|person") })
            contentType = MediaType.APPLICATION_JSON
            content = """{"from":"2026-08-04","to":"2026-08-03"}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `human access token with an operational scope cannot invoke an operational endpoint`() {
        mockMvc.post("/api/v1/fx-rates/sync") {
            with(
                jwt()
                    .jwt { it.subject("google-oauth2|person") }
                    .authorities(SimpleGrantedAuthority("SCOPE_fx:sync")),
            )
            contentType = MediaType.APPLICATION_JSON
            content = """{"from":"2026-08-04","to":"2026-08-03"}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `unapproved machine access token cannot invoke an operational endpoint`() {
        mockMvc.post("/api/v1/fx-rates/sync") {
            with(
                jwt()
                    .jwt { it.subject("unapproved-client@clients") }
                    .authorities(SimpleGrantedAuthority("SCOPE_fx:sync")),
            )
            contentType = MediaType.APPLICATION_JSON
            content = """{"from":"2026-08-04","to":"2026-08-03"}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `machine access token cannot invoke a personal financial endpoint`() {
        mockMvc.get("/api/v1/assets") {
            with(
                jwt()
                    .jwt { it.subject("approved-operations-client@clients") }
                    .authorities(SimpleGrantedAuthority("SCOPE_fx:sync")),
            )
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `machine access token with fx sync scope reaches the operational endpoint`() {
        mockMvc.post("/api/v1/fx-rates/sync") {
            with(
                jwt()
                    .jwt { it.subject("approved-operations-client@clients") }
                    .authorities(SimpleGrantedAuthority("SCOPE_fx:sync")),
            )
            contentType = MediaType.APPLICATION_JSON
            content = """{"from":"2026-08-04","to":"2026-08-03"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `asset access is isolated by the authenticated user`() {
        val created =
            mockMvc.post("/api/v1/assets") {
                with(userJwt("google-oauth2|owner-a", "allowed@example.com"))
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"Private asset","type":"CASH","liquidity":"LIQUID"}"""
            }.andExpect {
                status { isCreated() }
            }.andReturn()
        val assetId = objectMapper.readTree(created.response.contentAsString).get("id").asText()

        mockMvc.get("/api/v1/assets") {
            with(userJwt("google-oauth2|owner-b", "second@example.com"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }

        mockMvc.get("/api/v1/assets/$assetId") {
            with(userJwt("google-oauth2|owner-b", "second@example.com"))
        }.andExpect { status { isNotFound() } }

        mockMvc.put("/api/v1/assets/$assetId") {
            with(userJwt("google-oauth2|owner-b", "second@example.com"))
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Stolen","type":"CASH","liquidity":"LIQUID"}"""
        }.andExpect { status { isNotFound() } }

        mockMvc.post("/api/v1/assets/$assetId/archive") {
            with(userJwt("google-oauth2|owner-b", "second@example.com"))
        }.andExpect { status { isNotFound() } }

        mockMvc.get("/api/v1/assets/$assetId") {
            with(userJwt("google-oauth2|owner-a", "allowed@example.com"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Private asset") }
        }
    }

    @Test
    fun `liability access is isolated by the authenticated user`() {
        val created =
            mockMvc.post("/api/v1/liabilities") {
                with(userJwt("google-oauth2|liability-owner-a", "allowed@example.com"))
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"Private mortgage"}"""
            }.andExpect {
                status { isCreated() }
            }.andReturn()
        val liabilityId = objectMapper.readTree(created.response.contentAsString).get("id").asText()

        mockMvc.get("/api/v1/liabilities") {
            with(userJwt("google-oauth2|liability-owner-b", "second@example.com"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }

        mockMvc.get("/api/v1/liabilities/$liabilityId") {
            with(userJwt("google-oauth2|liability-owner-b", "second@example.com"))
        }.andExpect { status { isNotFound() } }

        mockMvc.put("/api/v1/liabilities/$liabilityId") {
            with(userJwt("google-oauth2|liability-owner-b", "second@example.com"))
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Stolen"}"""
        }.andExpect { status { isNotFound() } }

        mockMvc.post("/api/v1/liabilities/$liabilityId/archive") {
            with(userJwt("google-oauth2|liability-owner-b", "second@example.com"))
        }.andExpect { status { isNotFound() } }

        mockMvc.get("/api/v1/liabilities/$liabilityId") {
            with(userJwt("google-oauth2|liability-owner-a", "allowed@example.com"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Private mortgage") }
        }
    }

    @Test
    fun `snapshot and derived views are isolated by the authenticated user`() {
        val captured =
            mockMvc.post("/api/v1/snapshot-captures") {
                with(userJwt("google-oauth2|snapshot-owner-a", "allowed@example.com"))
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "asOf":"2026-08-04T00:00:00Z",
                      "recordedAt":"2026-08-04T01:00:00Z",
                      "baseCurrency":"TWD",
                      "assets":[{
                        "name":"Private cash",
                        "type":"CASH",
                        "liquidity":"LIQUID",
                        "money":{"amount":"100.00","currency":"TWD"},
                        "effectiveAt":"2026-08-04T00:00:00Z",
                        "source":"Private statement"
                      }],
                      "liabilities":[]
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
            }.andReturn()
        val snapshotId = objectMapper.readTree(captured.response.contentAsString).get("id").asText()

        mockMvc.get("/api/v1/snapshots") {
            with(userJwt("google-oauth2|snapshot-owner-b", "second@example.com"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }

        mockMvc.get("/api/v1/snapshots/$snapshotId") {
            with(userJwt("google-oauth2|snapshot-owner-b", "second@example.com"))
        }.andExpect { status { isNotFound() } }

        mockMvc.get("/api/v1/financial-health/$snapshotId") {
            with(userJwt("google-oauth2|snapshot-owner-b", "second@example.com"))
        }.andExpect { status { isNotFound() } }

        mockMvc.get("/api/v1/snapshots/$snapshotId") {
            with(userJwt("google-oauth2|snapshot-owner-a", "allowed@example.com"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.assets[0].name") { value("Private cash") }
        }
    }

    private fun userJwt(
        subject: String,
        email: String,
    ): RequestPostProcessor =
        jwt()
            .jwt {
                it.issuer("https://wealth-os-test.example/")
                    .subject(subject)
                    .claim("email", email)
                    .claim("email_verified", true)
            }.authorities(SimpleGrantedAuthority("SCOPE_wealth:access"))
}
