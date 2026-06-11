package com.economy.finance.api.exception;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiError {

    String code;
    String message;
    List<FieldViolation> fieldErrors;

    @Value
    @Builder
    public static class FieldViolation {
        String field;
        String message;
    }
}
