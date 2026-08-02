package com.wealthos.asset.adapter.http

import com.wealthos.asset.application.ArchiveAsset
import com.wealthos.asset.application.CreateAsset
import com.wealthos.asset.application.GetAsset
import com.wealthos.asset.application.ListAssets
import com.wealthos.asset.application.UpdateAsset
import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetId
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import com.wealthos.shared.adapter.http.ValidationProblemResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets")
class AssetController(
    private val listAssets: ListAssets,
    private val createAsset: CreateAsset,
    private val getAsset: GetAsset,
    private val updateAsset: UpdateAsset,
    private val archiveAsset: ArchiveAsset,
) {
    @GetMapping
    @Operation(summary = "List assets", operationId = "listAssets")
    fun list(): List<AssetResponse> = listAssets.execute().map(AssetResponse::from)

    @GetMapping("/{id}")
    @Operation(summary = "Get an asset", operationId = "getAsset")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Asset found",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = AssetResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Asset not found",
                content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    fun get(
        @PathVariable id: UUID,
    ): AssetResponse = AssetResponse.from(getAsset.execute(AssetId(id)))

    @PostMapping
    @Operation(summary = "Create an asset", operationId = "createAsset")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Asset created",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = AssetResponse::class))],
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
        @Valid @RequestBody request: CreateAssetRequest,
    ): ResponseEntity<AssetResponse> {
        val asset = createAsset.execute(request.name, request.type, request.liquidity)
        val response = AssetResponse.from(asset)

        return ResponseEntity.created(URI.create("/api/v1/assets/${response.id}")).body(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an asset", operationId = "updateAsset")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Asset updated", content = [Content(mediaType = "application/json", schema = Schema(implementation = AssetResponse::class))]),
            ApiResponse(responseCode = "400", description = "Request validation failed", content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ValidationProblemResponse::class))]),
            ApiResponse(responseCode = "404", description = "Asset not found", content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))]),
        ],
    )
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAssetRequest,
    ): AssetResponse =
        AssetResponse.from(
            updateAsset.execute(AssetId(id), request.name, request.type, request.liquidity),
        )

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an asset", operationId = "archiveAsset")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Asset archived"),
            ApiResponse(responseCode = "404", description = "Asset not found", content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))]),
        ],
    )
    fun archive(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        archiveAsset.execute(AssetId(id))
        return ResponseEntity.noContent().build()
    }
}

data class CreateAssetRequest(
    @field:NotBlank(message = "must not be blank")
    val name: String,
    val type: AssetType,
    val liquidity: Liquidity,
)

data class UpdateAssetRequest(
    @field:NotBlank(message = "must not be blank")
    val name: String,
    val type: AssetType,
    val liquidity: Liquidity,
)

data class AssetResponse(
    val id: String,
    val name: String,
    val type: String,
    val liquidity: String,
    val archived: Boolean,
) {
    companion object {
        fun from(asset: Asset): AssetResponse =
            AssetResponse(
                id = asset.id.value.toString(),
                name = asset.name,
                type = asset.type.name,
                liquidity = asset.liquidity.name,
                archived = asset.archived,
            )
    }
}
