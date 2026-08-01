package com.wealthos.liability.adapter.http

import com.wealthos.liability.application.LiabilityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class LiabilityExceptionHandler {
    @ExceptionHandler(LiabilityNotFoundException::class)
    fun handleNotFound(
        exception: LiabilityNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Liability was not found",
        ).apply {
            type = URI.create("urn:wealthos:problem:liability-not-found")
            title = "Liability not found"
            instance = URI.create(request.requestURI)
        }
}
