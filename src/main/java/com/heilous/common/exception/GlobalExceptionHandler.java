package com.heilous.common.exception;

import com.heilous.common.dto.APIResponse;
import com.heilous.common.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<ErrorResponse>>
    handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity.badRequest()
                .body(new APIResponse<>(400,
                        ErrorResponse.of("VALIDATION_ERROR", message)));
    }

    // JSON 파싱 실패 (잘못된 형식, 잘못된 enum 값 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<ErrorResponse>>
    handleHttpMessageNotReadable(HttpMessageNotReadableException e) {

        return ResponseEntity.badRequest()
                .body(new APIResponse<>(400,
                        ErrorResponse.of("INVALID_FORMAT", "요청 형식이 올바르지 않습니다. 필드 타입과 enum 값을 확인해주세요.")));
    }

    // 쿼리 파라미터 타입 불일치 (enum 값 오류 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<ErrorResponse>>
    handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {

        String message = String.format("'%s' 파라미터의 값이 올바르지 않습니다.", e.getName());

        return ResponseEntity.badRequest()
                .body(new APIResponse<>(400,
                        ErrorResponse.of("INVALID_PARAMETER", message)));
    }
}