package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Validates that a string value is a well-formed URL when the field is annotated with @SpeedyUrl.
 */
public class UrlRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;

        if (!val.isText()) {
            errors.add(fm.getOutputPropertyName() + " must be a valid URL");
            return;
        }
        String text = val.asText();
        try {
            new URL(text);
        } catch (MalformedURLException e) {
            errors.add(fm.getOutputPropertyName() + " must be a valid URL");
        }
    }
}
