package com.wealthos.asset.adapter.http

import com.wealthos.asset.application.CreateAsset
import com.wealthos.asset.application.ListAssets
import com.wealthos.asset.domain.Asset
import com.wealthos.asset.domain.AssetType
import com.wealthos.asset.domain.Liquidity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets")
class AssetController(
    private val listAssets: ListAssets,
    private val createAsset: CreateAsset,
) {
    @GetMapping
    @Operation(summary = "List assets")
    fun list(): List<AssetResponse> = listAssets.execute().map(AssetResponse::from)

    @PostMapping
    @Operation(summary = "Create an asset")
    fun create(
        @Valid @RequestBody request: CreateAssetRequest,
    ): ResponseEntity<AssetResponse> {
        val asset = createAsset.execute(request.name, request.type, request.liquidity)
        val response = AssetResponse.from(asset)

        return ResponseEntity.created(URI.create("/api/v1/assets/${response.id}")).body(response)
    }
}

data class CreateAssetRequest(
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
) {
    companion object {
        fun from(asset: Asset): AssetResponse =
            AssetResponse(
                id = asset.id.value.toString(),
                name = asset.name,
                type = asset.type.name,
                liquidity = asset.liquidity.name,
            )
    }
}
