package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Applies a single regex pattern check if the field has a ValidationSpec with code REGEX.
 */
public class RegexRule implements FieldRule {

    private final String pattern;

    public RegexRule(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;

        if (!val.isText() || !Pattern.matches(pattern, val.asText())) {
            errors.add(fm.getOutputPropertyName() + " does not match pattern " + pattern);
        }
    }
}
