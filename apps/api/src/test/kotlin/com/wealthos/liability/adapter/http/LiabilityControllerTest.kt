package com.wealthos.liability.adapter.http

import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:liability-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LiabilityControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `lists liabilities as transport responses`() {
        createLiability("Mortgage")
        createLiability("Credit Card")

        mockMvc.get("/api/v1/liabilities")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { isNotEmpty() }
                jsonPath("$[0].name") { value("Credit Card") }
                jsonPath("$[1].id") { isNotEmpty() }
                jsonPath("$[1].name") { value("Mortgage") }
            }
    }

    @Test
    fun `user can create a liability and retrieve it`() {
        val creationResult = mockMvc.post("/api/v1/liabilities") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "name": "Home Mortgage"
                }
                """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { string("Location", matchesPattern("/api/v1/liabilities/[0-9a-f-]{36}")) }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Home Mortgage") }
        }.andReturn()

        val location = requireNotNull(creationResult.response.getHeader("Location"))
        mockMvc.get(location)
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { isNotEmpty() }
                jsonPath("$.name") { value("Home Mortgage") }
            }
    }

    @Test
    fun `user can update a liability name`() {
        val creationResult =
            mockMvc.post("/api/v1/liabilities") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"Car Finance"}"""
            }.andReturn()
        val location = requireNotNull(creationResult.response.getHeader("Location"))

        mockMvc.put(location) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Vehicle Loan"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Vehicle Loan") }
        }

        mockMvc.get(location)
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Vehicle Loan") }
            }
    }

    @Test
    fun `user can archive a liability without removing its resource`() {
        val creationResult =
            mockMvc.post("/api/v1/liabilities") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"Settled Loan"}"""
            }.andReturn()
        val location = requireNotNull(creationResult.response.getHeader("Location"))

        mockMvc.post("$location/archive")
            .andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/liabilities")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }

        mockMvc.get(location)
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Settled Loan") }
                jsonPath("$.archived") { value(true) }
            }
    }

    @Test
    fun `blank liability name identifies the invalid field`() {
        mockMvc.post("/api/v1/liabilities") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "name": "   "
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
            jsonPath("$.type") { value("urn:wealthos:problem:validation-error") }
            jsonPath("$.title") { value("Request validation failed") }
            jsonPath("$.status") { value(400) }
            jsonPath("$.detail") { value("One or more fields are invalid") }
            jsonPath("$.instance") { value("/api/v1/liabilities") }
            jsonPath("$.errors.length()") { value(1) }
            jsonPath("$.errors[0].field") { value("name") }
            jsonPath("$.errors[0].message") { value("must not be blank") }
        }
    }

    @Test
    fun `missing liability name identifies the invalid request`() {
        mockMvc.post("/api/v1/liabilities") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
            jsonPath("$.type") { value("urn:wealthos:problem:validation-error") }
            jsonPath("$.title") { value("Request validation failed") }
            jsonPath("$.status") { value(400) }
            jsonPath("$.detail") { value("The request body is invalid") }
            jsonPath("$.instance") { value("/api/v1/liabilities") }
            jsonPath("$.errors.length()") { value(1) }
            jsonPath("$.errors[0].field") { value("request") }
            jsonPath("$.errors[0].message") { value("must be valid JSON with all required fields") }
        }
    }

    @Test
    fun `missing liability returns a not found problem`() {
        val missingId = "0f27e4fa-99f8-4c5e-87da-527488cbe515"

        mockMvc.get("/api/v1/liabilities/$missingId")
            .andExpect {
                status { isNotFound() }
                content { contentType("application/problem+json") }
                jsonPath("$.type") { value("urn:wealthos:problem:liability-not-found") }
                jsonPath("$.title") { value("Liability not found") }
                jsonPath("$.status") { value(404) }
                jsonPath("$.detail") { value("Liability $missingId was not found") }
                jsonPath("$.instance") { value("/api/v1/liabilities/$missingId") }
            }
    }

    @Test
    fun `invalid liability identifier identifies the invalid field`() {
        mockMvc.get("/api/v1/liabilities/not-a-uuid")
            .andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
                jsonPath("$.type") { value("urn:wealthos:problem:validation-error") }
                jsonPath("$.title") { value("Request validation failed") }
                jsonPath("$.status") { value(400) }
                jsonPath("$.detail") { value("One or more fields are invalid") }
                jsonPath("$.instance") { value("/api/v1/liabilities/not-a-uuid") }
                jsonPath("$.errors.length()") { value(1) }
                jsonPath("$.errors[0].field") { value("id") }
                jsonPath("$.errors[0].message") { value("must be a valid UUID") }
            }
    }

    private fun createLiability(name: String) {
        mockMvc.post("/api/v1/liabilities") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name"}"""
        }.andExpect {
            status { isCreated() }
        }
    }
}
