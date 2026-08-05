package com.wealthos.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiDocumentationConfiguration {
    @Bean
    fun wealthOsOpenApi(): OpenAPI =
        OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        USER_BEARER,
                        bearerScheme("Auth0 user access token for personal financial APIs."),
                    )
                    .addSecuritySchemes(
                        OPERATIONAL_M2M_BEARER,
                        bearerScheme("Auth0 machine access token requiring the fx:sync authority."),
                    ),
            )
            .addSecurityItem(SecurityRequirement().addList(USER_BEARER))
            .info(
                Info()
                    .title("Wealth OS API")
                    .version("v1")
                    .description("Contract between the Wealth OS web application and backend."),
            )

    private fun bearerScheme(description: String): SecurityScheme =
        SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description(description)

    companion object {
        const val USER_BEARER = "userBearer"
        const val OPERATIONAL_M2M_BEARER = "operationalM2mBearer"
    }
}
