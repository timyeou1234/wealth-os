package com.wealthos.configuration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:api-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc
class ApiSecurityBoundaryTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `personal financial API rejects an unauthenticated request`() {
        mockMvc.get("/api/v1/assets")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `authenticated user can invoke a personal financial endpoint`() {
        mockMvc.get("/api/v1/assets") {
            with(jwt().jwt { it.subject("google-oauth2|person") })
        }.andExpect { status { isOk() } }
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
}
