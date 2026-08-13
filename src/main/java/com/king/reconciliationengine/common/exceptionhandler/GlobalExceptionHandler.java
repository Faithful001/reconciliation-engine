package com.king.reconciliationengine.common.exceptionhandler;

import com.king.reconciliationengine.common.response.Response;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<List<Map<String, String>>>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        return new ResponseEntity<>(
                Response.error("Validation failed"),
                ex.getStatusCode()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response<Object>> handleStatus(ResponseStatusException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getReason()),
                HttpStatus.valueOf(ex.getStatusCode().value())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
                Response.error("Data conflict: " + ex.getMostSpecificCause().getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Object>> handleGeneric(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal Server Error";
        return new ResponseEntity<>(
                Response.error(message),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Response<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return new ResponseEntity<>(
                Response.error("Method not allowed: " + ex.getMethod()),
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }
}