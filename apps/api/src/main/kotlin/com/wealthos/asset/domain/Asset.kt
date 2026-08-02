package com.wealthos.asset.domain

class Asset(
    val id: AssetId,
    name: String,
    val type: AssetType,
    val liquidity: Liquidity,
    val archived: Boolean = false,
) {
    val name: String = name.trim()

    init {
        require(this.name.isNotEmpty()) { "Asset name must not be blank" }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is Asset && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}
