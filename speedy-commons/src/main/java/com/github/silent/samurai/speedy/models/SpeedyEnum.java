package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.Objects;

/**
 * Type-agnostic Enum wrapper that delegates to either SpeedyEnumStr (STRING mode)
 * or SpeedyEnumOrd (ORDINAL mode) based on FieldMetadata#getEnumMode().
 */
public class SpeedyEnum implements SpeedyValue {

    private final FieldMetadata fieldMetadata;
    private final SpeedyValue delegate; // SpeedyEnumStr or SpeedyEnumOrd
    private final EnumMode enumMode;

    public SpeedyEnum(String value, FieldMetadata fieldMetadata) throws BadRequestException {
        if (fieldMetadata == null) throw new BadRequestException("FieldMetadata cannot be null for enum");
        if (value == null) throw new BadRequestException("Enum value cannot be null");
        this.enumMode = fieldMetadata.getOperationalEnumMode();
        if (enumMode != EnumMode.STRING) {
            throw new BadRequestException("SpeedyEnum (String) expects STRING mode but found " + enumMode);
        }
        this.fieldMetadata = fieldMetadata;
        validateValue(value, fieldMetadata);
        this.delegate = new SpeedyText(value);
    }

    public SpeedyEnum(Long value, FieldMetadata fieldMetadata) throws BadRequestException {
        if (fieldMetadata == null) throw new BadRequestException("FieldMetadata cannot be null for enum");
        if (value == null) throw new BadRequestException("Enum value cannot be null");
        this.enumMode = fieldMetadata.getOperationalEnumMode();
        if (enumMode != EnumMode.ORDINAL) {
            throw new BadRequestException("SpeedyEnum (Integer) expects ORDINAL mode but found " + enumMode);
        }
        this.fieldMetadata = fieldMetadata;
        validateValue(value, fieldMetadata);
        this.delegate = new SpeedyInt(value);
    }

    private void validateValue(Long value, FieldMetadata fieldMetadata) throws BadRequestException {
        fieldMetadata.getDynamicEnum()
                .fromCode(value.intValue())
                .orElseThrow(() -> new BadRequestException("Enum ordinal value '" + value + "' is not valid."));
    }

    private void validateValue(String value, FieldMetadata fieldMetadata) throws BadRequestException {
        if (enumMode != EnumMode.STRING) {
            throw new BadRequestException("SpeedyEnum expects STRING mode but found " + enumMode);
        }
        fieldMetadata.getDynamicEnum()
                .fromName(value)
                .orElseThrow(() -> new BadRequestException("Enum value '" + value + "' is not valid."));
    }

    @Override
    public ValueType getValueType() {
        if (enumMode == EnumMode.ORDINAL) return ValueType.ENUM_ORD;
        return ValueType.ENUM;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public String asText() {
        return delegate.asText();
    }

    @Override
    public Long asInt() {
        return delegate.asInt();
    }

    @Override
    public Long asEnumOrd() {
        return delegate.asInt();
    }

    @Override
    public String asEnum() {
        return delegate.asText();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeedyEnum that = (SpeedyEnum) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }
}
