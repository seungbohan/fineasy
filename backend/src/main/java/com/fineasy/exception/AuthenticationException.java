package com.fineasy.exception;

public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }
}
