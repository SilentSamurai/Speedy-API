package com.github.silent.samurai.speedy.file.impl.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.*;
import java.util.Set;

public class JsonValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonValidator.class);

    public static <T> void validate(T jsonField) {
        // Obtain a ValidatorFactory and create a Validator
        Validator validator;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        // Validate the JsonField object
        Set<ConstraintViolation<T>> violations = validator.validate(jsonField);

        // Check if there are any violations
        if (!violations.isEmpty()) {
            // Handle the validation errors
            for (ConstraintViolation<T> violation : violations) {

                LOGGER.error("Validation error: {} ", violation);
            }

            // Optionally, you can throw an exception if validation fails
            throw new ConstraintViolationException(violations);
        } else {
            LOGGER.info("JsonField object is valid!");
        }
    }

}
