package com.economy.finance.api.exception;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiError.builder()
                                .code("UNAUTHORIZED")
                                .message(ex.getMessage())
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiError.builder()
                                .code("BAD_REQUEST")
                                .message("Corpo da requisição inválido")
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Violação de integridade ao persistir dados", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.builder()
                                .code("CONFLICT")
                                .message("Não foi possível salvar: verifique conta, categoria e migrações da base.")
                                .fieldErrors(List.of())
                                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiError.builder()
                                .code("INTERNAL_ERROR")
                                .message("Erro interno")
                                .fieldErrors(List.of())
                                .build());
    }
}
