package com.fineasy.external.kis;

public class KisApiException extends RuntimeException {

    private final String errorCode;

    private final int httpStatus;

    public KisApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 0;
    }

    public KisApiException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public KisApiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = 0;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
