package com.wealthos.liability.adapter.http

import com.wealthos.domain.liability.Liability
import com.wealthos.domain.liability.LiabilityId
import com.wealthos.liability.application.CreateLiability
import com.wealthos.liability.application.GetLiability
import com.wealthos.liability.application.ListLiabilities
import com.wealthos.shared.adapter.http.ValidationProblemResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/v1/liabilities")
@Tag(name = "Liabilities")
class LiabilityController(
    private val listLiabilities: ListLiabilities,
    private val createLiability: CreateLiability,
    private val getLiability: GetLiability,
) {
    @GetMapping
    @Operation(summary = "List liabilities")
    fun list(): List<LiabilityResponse> = listLiabilities.execute().map(LiabilityResponse::from)

    @PostMapping
    @Operation(summary = "Create a liability")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Liability created",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = LiabilityResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request validation failed",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ValidationProblemResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun create(
        @Valid @RequestBody request: CreateLiabilityRequest,
    ): ResponseEntity<LiabilityResponse> {
        val response = LiabilityResponse.from(createLiability.execute(request.name))

        return ResponseEntity.created(URI.create("/api/v1/liabilities/${response.id}")).body(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a liability")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liability found",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = LiabilityResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Liability not found",
                content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    fun get(
        @PathVariable id: UUID,
    ): LiabilityResponse = LiabilityResponse.from(getLiability.execute(LiabilityId(id)))
}

data class CreateLiabilityRequest(
    @field:NotBlank(message = "must not be blank")
    val name: String,
)

data class LiabilityResponse(
    val id: String,
    val name: String,
) {
    companion object {
        fun from(liability: Liability): LiabilityResponse =
            LiabilityResponse(
                id = liability.id.value.toString(),
                name = liability.name,
            )
    }
}
