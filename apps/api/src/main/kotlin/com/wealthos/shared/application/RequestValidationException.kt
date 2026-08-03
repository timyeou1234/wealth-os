package com.wealthos.shared.application

data class RequestFieldError(
    val field: String,
    val message: String,
)

class RequestValidationException(
    val errors: List<RequestFieldError>,
) : RuntimeException("Request validation failed") {
    constructor(field: String, message: String) : this(listOf(RequestFieldError(field, message)))
}
