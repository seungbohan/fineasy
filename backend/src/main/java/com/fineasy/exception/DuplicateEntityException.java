package com.fineasy.exception;

public class DuplicateEntityException extends BusinessException {

    public DuplicateEntityException(String entityName, String field, Object value) {
        super("DUPLICATE_ENTITY",
                entityName + " already exists with " + field + ": " + value);
    }
}
