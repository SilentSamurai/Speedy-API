package com.github.silent.samurai.speedy.types;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {

    @Override
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.toString();
    }

    @Override
    public Email convertToEntityAttribute(String s) {
        return s == null ? null : new Email(s);
    }
}
