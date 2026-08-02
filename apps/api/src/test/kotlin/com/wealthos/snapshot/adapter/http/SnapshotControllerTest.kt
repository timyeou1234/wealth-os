package com.wealthos.snapshot.adapter.http

import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:snapshot-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc
@Transactional
class SnapshotControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `user can create an empty snapshot and retrieve it`() {
        val result = mockMvc.post("/api/v1/snapshots") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"asOf":"2026-08-01T00:00:00Z","recordedAt":"2026-08-01T00:00:00Z","assets":[],"liabilities":[]}
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { string("Location", matchesPattern("/api/v1/snapshots/[0-9a-f-]{36}")) }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.asOf") { value("2026-08-01T00:00:00Z") }
            jsonPath("$.assets.length()") { value(0) }
            jsonPath("$.liabilities.length()") { value(0) }
        }.andReturn()

        val location = requireNotNull(result.response.getHeader("Location"))
        mockMvc.get(location)
            .andExpect {
                status { isOk() }
                jsonPath("$.asOf") { value("2026-08-01T00:00:00Z") }
                jsonPath("$.assets.length()") { value(0) }
                jsonPath("$.liabilities.length()") { value(0) }
            }
    }

    @Test
    fun `lists effective snapshots in chronological order`() {
        createSnapshot("2026-07-01T00:00:00Z")
        createSnapshot("2026-08-01T00:00:00Z")

        mockMvc.get("/api/v1/snapshots")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].asOf") { value("2026-07-01T00:00:00Z") }
                jsonPath("$[1].asOf") { value("2026-08-01T00:00:00Z") }
            }
    }

    private fun createSnapshot(asOf: String) {
        mockMvc.post("/api/v1/snapshots") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"asOf":"$asOf","recordedAt":"$asOf","assets":[],"liabilities":[]}"""
        }.andExpect {
            status { isCreated() }
        }
    }
}
