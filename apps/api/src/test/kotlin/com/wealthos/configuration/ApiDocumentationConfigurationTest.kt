package com.wealthos.configuration

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:api-documentation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    ],
)
@AutoConfigureMockMvc
class ApiDocumentationConfigurationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `publishes the OpenAPI contract`() {
        mockMvc.get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                content { string(containsString("\"title\":\"Wealth OS API\"")) }
                content { string(containsString("\"version\":\"v1\"")) }
            }
    }

    @Test
    fun `documents separate user and operational authentication boundaries`() {
        mockMvc.get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.components.securitySchemes.userBearer.type") { value("http") }
                jsonPath("$.components.securitySchemes.userBearer.scheme") { value("bearer") }
                jsonPath("$.components.securitySchemes.operationalM2mBearer.type") { value("http") }
                jsonPath("$.components.securitySchemes.operationalM2mBearer.scheme") { value("bearer") }
                jsonPath("$.security[0].userBearer") { exists() }
                jsonPath("$.paths['/api/v1/fx-rates/sync'].post.security[0].operationalM2mBearer") { exists() }
            }
    }

    @Test
    fun `documents asset creation success and validation responses`() {
        mockMvc.get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.paths['/api/v1/assets'].post.responses['201']") { exists() }
                jsonPath("$.paths['/api/v1/assets'].post.responses['400'].content['application/problem+json']") {
                    exists()
                }
                jsonPath(
                    "$.paths['/api/v1/assets'].post.responses['400'].content['application/problem+json'].schema['\$ref']",
                ) {
                    value("#/components/schemas/ValidationProblemResponse")
                }
                jsonPath("$.components.schemas.ValidationProblemResponse.properties.errors") { exists() }
                jsonPath("$.components.schemas.FieldValidationError.properties.field") { exists() }
                jsonPath("$.components.schemas.FieldValidationError.properties.message") { exists() }
                jsonPath("$.paths['/api/v1/assets'].post.responses['200']") { doesNotExist() }
                jsonPath("$.paths['/api/v1/assets/{id}'].get.responses['200']") { exists() }
                jsonPath("$.paths['/api/v1/assets/{id}'].get.responses['404'].content['application/problem+json']") {
                    exists()
                }
            }
    }

    @Test
    fun `documents asset and liability lifecycle operations`() {
        mockMvc.get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.paths['/api/v1/assets'].get.operationId") { value("listAssets") }
                jsonPath("$.paths['/api/v1/assets/{id}'].get.operationId") { value("getAsset") }
                jsonPath("$.paths['/api/v1/assets/{id}'].put.operationId") { value("updateAsset") }
                jsonPath("$.paths['/api/v1/assets/{id}/archive'].post.operationId") { value("archiveAsset") }
                jsonPath("$.paths['/api/v1/assets/{id}'].put.responses['200']") { exists() }
                jsonPath("$.paths['/api/v1/assets/{id}'].put.responses['200'].content['application/json'].schema['\$ref']") { value("#/components/schemas/AssetResponse") }
                jsonPath("$.paths['/api/v1/assets/{id}'].put.responses['400']") { exists() }
                jsonPath("$.paths['/api/v1/assets/{id}'].put.responses['404']") { exists() }
                jsonPath("$.paths['/api/v1/assets/{id}/archive'].post.responses['204']") { exists() }
                jsonPath("$.paths['/api/v1/assets/{id}/archive'].post.responses['404']") { exists() }
                jsonPath("$.paths['/api/v1/liabilities/{id}'].put.responses['200']") { exists() }
                jsonPath("$.paths['/api/v1/liabilities/{id}'].put.responses['200'].content['application/json'].schema['\$ref']") { value("#/components/schemas/LiabilityResponse") }
                jsonPath("$.paths['/api/v1/liabilities/{id}'].put.responses['400']") { exists() }
                jsonPath("$.paths['/api/v1/liabilities/{id}'].put.responses['404']") { exists() }
                jsonPath("$.paths['/api/v1/liabilities/{id}/archive'].post.responses['204']") { exists() }
                jsonPath("$.paths['/api/v1/liabilities/{id}/archive'].post.responses['404']") { exists() }
                jsonPath("$.paths['/api/v1/liabilities'].get.operationId") { value("listLiabilities") }
                jsonPath("$.paths['/api/v1/liabilities/{id}'].get.operationId") { value("getLiability") }
                jsonPath("$.paths['/api/v1/liabilities/{id}'].put.operationId") { value("updateLiability") }
                jsonPath("$.paths['/api/v1/liabilities/{id}/archive'].post.operationId") { value("archiveLiability") }
                jsonPath("$.paths['/api/v1/snapshots'].get.operationId") { value("listSnapshots") }
                jsonPath("$.paths['/api/v1/snapshots/{id}'].get.operationId") { value("getSnapshot") }
                jsonPath("$.paths['/api/v1/financial-health/{snapshotId}'].get.operationId") {
                    value("getFinancialHealth")
                }
            }
    }

    @Test
    fun `documents atomic snapshot capture`() {
        mockMvc.get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.paths['/api/v1/snapshot-captures'].post.operationId") { value("captureSnapshot") }
                jsonPath("$.paths['/api/v1/snapshot-captures'].post.responses['201'].content['application/json'].schema['\$ref']") {
                    value("#/components/schemas/SnapshotResponse")
                }
                jsonPath("$.paths['/api/v1/snapshot-captures'].post.responses['400'].content['application/problem+json'].schema['\$ref']") {
                    value("#/components/schemas/ValidationProblemResponse")
                }
                jsonPath("$.paths['/api/v1/snapshot-captures'].post.responses['200']") { doesNotExist() }
                jsonPath("$.components.schemas.CaptureSnapshotRequest.required") {
                    value(org.hamcrest.Matchers.hasItems("assets", "liabilities"))
                }
                jsonPath("$.components.schemas.CaptureAssetRequest.properties.originalMoney") { exists() }
                jsonPath("$.components.schemas.CaptureAssetRequest.properties.declaredRate") { exists() }
                jsonPath("$.components.schemas.AssetFactResponse.properties.appliedConversion") { exists() }
                jsonPath("$.components.schemas.AppliedConversionResponse.properties.rateDate") { exists() }
            }
    }
}
