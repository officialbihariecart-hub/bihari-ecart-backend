package com.bihariecart.exception;

/**
 * Exception thrown when a user attempts to submit a duplicate review for the same product.
 */
public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(String message) {
        super(message);
    }
}
