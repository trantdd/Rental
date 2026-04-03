package com.example.rental.exception;

import lombok.Data;

@Data
public class AppException extends RuntimeException {

    public AppException(ErrorCode errorCode) {
        super((errorCode.getMessage()));
        this.errorCode = errorCode;
    }

    private ErrorCode errorCode;

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
