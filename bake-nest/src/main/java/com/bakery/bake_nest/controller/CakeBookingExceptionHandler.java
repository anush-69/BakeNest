package com.bakery.bake_nest.controller;

import com.bakery.bake_nest.dto.CakeBookingApiError;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = CakeBookingController.class)
@Profile("booking-local")
public class CakeBookingExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CakeBookingApiError> invalidArgument(IllegalArgumentException exception) {
        // Expose only the service's known public messages.
        String message = exception.getMessage();
        if ("Booking not found for this customer.".equals(message)) {
            return error(HttpStatus.NOT_FOUND, message);
        }
        if ("A valid booking ID must be provided.".equals(message)
                || "An existing customer must be provided.".equals(message)
                || "Page must be zero or greater.".equals(message)
                || "Page size must be between 1 and 50.".equals(message)
                || "A booking request with a required date must be provided.".equals(message)) {
            return error(HttpStatus.BAD_REQUEST, message);
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process booking request.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CakeBookingApiError> invalidState(IllegalStateException exception) {
        String message = exception.getMessage();
        if ("Only requested bookings can be updated.".equals(message)
                || "Only requested bookings can be cancelled.".equals(message)) {
            return error(HttpStatus.CONFLICT, message);
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process booking request.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CakeBookingApiError> unauthenticated() {
        return error(HttpStatus.UNAUTHORIZED, "Authentication required.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CakeBookingApiError> forbidden() {
        return error(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<CakeBookingApiError> unreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Invalid booking request.");
    }

    private ResponseEntity<CakeBookingApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new CakeBookingApiError(status.value(), message));
    }
}
