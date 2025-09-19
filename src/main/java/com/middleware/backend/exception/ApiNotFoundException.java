// ApiNotFoundException.java
package com.middleware.backend.exception;

/**
 * Exception indicating a requested API resource could not be found.
 * <p>
 * Intended for signaling 404-like conditions from service/controller layers.
 */
public class ApiNotFoundException extends RuntimeException {
    public ApiNotFoundException(String message) {
        super(message);
       
    }

}
