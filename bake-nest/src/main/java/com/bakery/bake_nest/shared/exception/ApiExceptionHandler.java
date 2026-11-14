package com.bakery.bake_nest.shared.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidJson() {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON or field value."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(
                        Map.of(
                                "message",
                                exception.getReason() == null
                                        ? "Request failed."
                                        : exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception) {
        var error = exception.getBindingResult().getFieldErrors().getFirst();
        return ResponseEntity.badRequest()
                .body(Map.of("message", error.getField() + ": " + error.getDefaultMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDatabaseConflict(DataIntegrityViolationException exception) {
        // Do not send SQL, passwords or database details to clients.
        return ResponseEntity.status(409)
                .body(Map.of("message", "A value already exists or the record is still in use."));
    }
}
