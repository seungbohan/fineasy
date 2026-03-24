package com.fineasy.exception;

public class ProfanityException extends BusinessException {

    public ProfanityException() {
        super("PROFANITY_DETECTED", "Content contains prohibited words");
    }
}
