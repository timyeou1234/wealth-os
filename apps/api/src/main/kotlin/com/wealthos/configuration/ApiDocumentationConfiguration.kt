package com.wealthos.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiDocumentationConfiguration {
    @Bean
    fun wealthOsOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Wealth OS API")
                .version("v1")
                .description("Contract between the Wealth OS web application and backend."),
        )
}
