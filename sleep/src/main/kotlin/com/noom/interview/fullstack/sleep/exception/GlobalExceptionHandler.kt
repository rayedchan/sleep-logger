package com.noom.interview.fullstack.sleep.exception

import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(ex: MissingRequestHeaderException): ResponseEntity<Map<String, String>> {
        val errorBody = mapOf(
            "message" to "Required header '${ex.headerName}' is missing. Please provide a valid UUID in the '${ex.headerName}' header.",
            "status" to "400"
        )
        return ResponseEntity(errorBody, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKey(ex: DuplicateKeyException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "message" to "You have already logged sleep for this date. Only one log per day is allowed.",
            "status" to HttpStatus.CONFLICT.value()
        )

        return ResponseEntity(body, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "timestamp" to java.time.LocalDateTime.now().toString(),
            "status" to ex.status.value(),
            "error" to ex.status.reasonPhrase,
            "message" to (ex.reason ?: "No message provided"),
            "path" to "/api/v1/sleep"
        )
        return ResponseEntity(body, ex.status)
    }
}