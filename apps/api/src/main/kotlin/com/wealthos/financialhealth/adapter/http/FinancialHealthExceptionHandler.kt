package com.wealthos.financialhealth.adapter.http

import com.wealthos.financialhealth.application.SnapshotNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class FinancialHealthExceptionHandler {
    @ExceptionHandler(SnapshotNotFoundException::class)
    fun handleNotFound(
        exception: SnapshotNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.message ?: "Snapshot was not found").apply {
            type = URI.create("urn:wealthos:problem:snapshot-not-found")
            title = "Snapshot not found"
            instance = URI.create(request.requestURI)
        }
}
