package com.wealthos.shared.adapter.http

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "One or more fields are invalid",
        ).apply {
            type = URI.create("urn:wealthos:problem:validation-error")
            title = "Request validation failed"
            instance = URI.create(request.requestURI)
            setProperty(
                "errors",
                exception.bindingResult.fieldErrors
                    .map { FieldValidationError(field = it.field, message = it.defaultMessage ?: "is invalid") }
                    .sortedBy { it.field },
            )
        }
}

data class FieldValidationError(
    val field: String,
    val message: String,
)
