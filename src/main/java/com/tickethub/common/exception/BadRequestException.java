package com.tickethub.common.exception;

// For invalid input that needs the database to detect (annotations can't check it)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}