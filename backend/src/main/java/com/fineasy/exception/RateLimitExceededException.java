package com.fineasy.exception;

public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", message);
    }
}
