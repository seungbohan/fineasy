package com.fineasy.exception;

public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entityName, Object identifier) {
        super("ENTITY_NOT_FOUND",
                entityName + " not found with identifier: " + identifier);
    }
}
