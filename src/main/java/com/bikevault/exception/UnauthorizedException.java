package com.bikevault.exception;

/**
 * Raised when a signed-in user attempts an operation their role cannot perform.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
