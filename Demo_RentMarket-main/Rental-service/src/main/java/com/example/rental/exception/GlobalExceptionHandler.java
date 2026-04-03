package com.example.rental.exception;

import com.example.rental.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Xử lý ngoại lệ toàn cục cho Rental-service.
 * Đảm bảo mọi lỗi đều trả về định dạng ApiResponse nhất quán.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Xử lý lỗi 403 — không có quyền */
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<?>> handlingAccessDenied(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED_ACCESS;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /** Xử lý lỗi 401 — chưa đăng nhập */
    @ExceptionHandler(value = AuthenticationException.class)
    ResponseEntity<ApiResponse<?>> handlingAuthentication(AuthenticationException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /** Xử lý AppException — lỗi nghiệp vụ */
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /** Xử lý lỗi validation (@Valid) */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException e) {
        String message = (e.getFieldError() != null)
                ? e.getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_DATA.getMessage();

        return ResponseEntity
                .status(ErrorCode.INVALID_DATA.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(ErrorCode.INVALID_DATA.getCode())
                        .message(message)
                        .build());
    }

    /** Xử lý lỗi không mong đợi */
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingRuntimeException(Exception e) {
        log.error("Lỗi không xác định: ", e);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
