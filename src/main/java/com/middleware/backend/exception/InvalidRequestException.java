// InvalidRequestException.java
package com.middleware.backend.exception;

/**
 * Exception indicating a client-supplied request was invalid.
 * <p>
 * Useful for mapping to 400 Bad Request responses.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
