package org.ostad.assignment_23.exception;

public class EmailRateLimitException extends RuntimeException {
    public EmailRateLimitException(String message) {
        super(message);
    }
}
