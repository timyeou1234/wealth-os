package com.wealthos.shared.adapter.http

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<ValidationProblemResponse> {
        val problem =
            ValidationProblemResponse(
                type = URI.create("urn:wealthos:problem:validation-error"),
                title = "Request validation failed",
                status = HttpStatus.BAD_REQUEST.value(),
                detail = "One or more fields are invalid",
                instance = URI.create(request.requestURI),
                errors =
                exception.bindingResult.fieldErrors
                    .map { FieldValidationError(field = it.field, message = it.defaultMessage ?: "is invalid") }
                    .sortedBy { it.field },
            )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem)
    }
}

data class FieldValidationError(
    val field: String,
    val message: String,
)

data class ValidationProblemResponse(
    val type: URI,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: URI,
    val errors: List<FieldValidationError>,
)
