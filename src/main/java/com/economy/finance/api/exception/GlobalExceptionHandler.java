package com.economy.finance.api.exception;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var violations =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(this::toViolation)
                        .collect(Collectors.toList());
        ApiError body =
                ApiError.builder()
                        .code("VALIDATION_ERROR")
                        .message("Dados inválidos")
                        .fieldErrors(violations)
                        .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ApiError.FieldViolation toViolation(FieldError fe) {
        return ApiError.FieldViolation.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.builder()
                                .code("CONFLICT")
                                .message(ex.getMessage())
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiError.builder()
                                .code("NOT_FOUND")
                                .message(ex.getMessage())
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiError.builder()
                                .code("INVALID_CREDENTIALS")
                                .message("Email ou palavra-passe inválidos")
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiError.builder()
                                .code("BAD_REQUEST")
                                .message(ex.getMessage())
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiError.builder()
                                .code("INTERNAL_ERROR")
                                .message("Erro interno")
                                .fieldErrors(List.of())
                                .build());
    }
}
