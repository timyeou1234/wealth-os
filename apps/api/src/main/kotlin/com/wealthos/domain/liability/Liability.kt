package com.wealthos.domain.liability

class Liability(
    val id: LiabilityId,
    name: String,
) {
    val name: String = name.trim()

    init {
        require(this.name.isNotEmpty()) { "Liability name must not be blank" }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is Liability && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}
