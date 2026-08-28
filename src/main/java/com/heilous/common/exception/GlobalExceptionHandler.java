package com.heilous.common.exception;

import com.heilous.common.dto.APIResponse;
import com.heilous.common.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<APIResponse<ErrorResponse>>
    handleCustomException(CustomException e) {

        GlobalErrorCode errorCode = e.getErrorCode();

        ErrorResponse errorResponse =
                ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage()
                );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        new APIResponse<>(
                                errorCode.getStatus(),
                                errorResponse
                        )
                );
    }

    // DataIntegrityViolation 예외 처리 (FK 제약조건 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse<ErrorResponse>>
    handleDataIntegrityViolation(DataIntegrityViolationException e) {

        GlobalErrorCode errorCode = GlobalErrorCode.DATA_INTEGRITY_VIOLATION;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new APIResponse<>(
                        errorCode.getStatus(),
                        ErrorResponse.of(errorCode.getCode(), errorCode.getMessage())
                ));
    }

    // Validation 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)    public ResponseEntity<APIResponse<ErrorResponse>>
    handleValidationException(
            MethodArgumentNotValidException e
    ) {

        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        ErrorResponse errorResponse =
                ErrorResponse.of(
                        "VALIDATION_ERROR",
                        message
                );

        return ResponseEntity.badRequest()
                .body(
                        new APIResponse<>(
                                400,
                                errorResponse
                        )
                );
    }
}