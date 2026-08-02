package com.wealthos.shared.adapter.http

import com.wealthos.shared.application.RequestValidationException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(RequestValidationException::class)
    fun handleRequestValidation(
        exception: RequestValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ValidationProblemResponse> =
        validationProblem(
            request,
            "One or more fields are invalid",
            exception.errors.map { FieldValidationError(it.field, it.message) },
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidDomainInput(request: HttpServletRequest): ResponseEntity<ValidationProblemResponse> =
        validationProblem(request, "One or more fields are invalid", listOf(FieldValidationError("request", "contains invalid values")))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ValidationProblemResponse> =
        validationProblem(
            request = request,
            detail = "One or more fields are invalid",
            errors = listOf(FieldValidationError(field = exception.name, message = "must be a valid UUID")),
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(
        request: HttpServletRequest,
    ): ResponseEntity<ValidationProblemResponse> =
        validationProblem(
            request = request,
            detail = "The request body is invalid",
            errors = listOf(FieldValidationError(field = "request", message = "must be valid JSON with all required fields")),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ValidationProblemResponse> {
        return validationProblem(
            request = request,
            detail = "One or more fields are invalid",
            errors =
            exception.bindingResult.fieldErrors
                .map { FieldValidationError(field = it.field, message = it.defaultMessage ?: "is invalid") }
                .sortedBy { it.field },
        )
    }

    private fun validationProblem(
        request: HttpServletRequest,
        detail: String,
        errors: List<FieldValidationError>,
    ): ResponseEntity<ValidationProblemResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(
                ValidationProblemResponse(
                    type = URI.create("urn:wealthos:problem:validation-error"),
                    title = "Request validation failed",
                    status = HttpStatus.BAD_REQUEST.value(),
                    detail = detail,
                    instance = URI.create(request.requestURI),
                    errors = errors,
                ),
            )
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
