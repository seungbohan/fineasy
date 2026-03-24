package com.fineasy.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
