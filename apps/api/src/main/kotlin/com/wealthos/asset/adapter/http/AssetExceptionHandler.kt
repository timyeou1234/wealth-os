package com.wealthos.asset.adapter.http

import com.wealthos.asset.application.AssetNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class AssetExceptionHandler {
    @ExceptionHandler(AssetNotFoundException::class)
    fun handleNotFound(
        exception: AssetNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            exception.message ?: "Asset was not found",
        ).apply {
            type = URI.create("urn:wealthos:problem:asset-not-found")
            title = "Asset not found"
            instance = URI.create(request.requestURI)
        }
}
