package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to perform Jakarta Bean Validation on any object.
 */
public final class ValidationUtils {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private ValidationUtils() {
        // utility class
    }

    public static <T> void validate(T object) throws BadRequestException {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Validation failed: " + message);
        }
    }
}
