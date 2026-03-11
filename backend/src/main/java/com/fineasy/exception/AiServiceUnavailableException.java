package com.fineasy.exception;

public class AiServiceUnavailableException extends BusinessException {

    public AiServiceUnavailableException(String message) {
        super("AI_SERVICE_UNAVAILABLE", message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super("AI_SERVICE_UNAVAILABLE", message, cause);
    }
}
