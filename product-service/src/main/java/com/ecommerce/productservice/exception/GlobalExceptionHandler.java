package com.ecommerce.productservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is malformed.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
