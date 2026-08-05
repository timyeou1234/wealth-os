package com.wealthos.configuration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:api-documentation-defaults;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@AutoConfigureMockMvc
class ApiDocumentationProductionDefaultsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `Swagger is disabled by default`() {
        mockMvc.get("/v3/api-docs") {
            with(jwt().jwt { it.subject("google-oauth2|person") })
        }.andExpect { status { isNotFound() } }
    }
}
