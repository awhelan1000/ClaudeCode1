package com.marlowefinch.ops;

import java.util.List;

/**
 * Thrown when one or more request parameters fail validation. Carries every
 * problem found for the request so {@link ApiExceptionHandler} can report them
 * together, see TODO-232.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
