package com.wealthos.asset.adapter.http

import com.wealthos.asset.application.ListAssets
import com.wealthos.asset.domain.Asset
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets")
class AssetController(
    private val listAssets: ListAssets,
) {
    @GetMapping
    @Operation(summary = "List assets")
    fun list(): List<AssetResponse> = listAssets.execute().map(AssetResponse::from)
}

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
