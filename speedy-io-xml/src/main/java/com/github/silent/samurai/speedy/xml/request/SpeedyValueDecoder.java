package com.github.silent.samurai.speedy.xml.request;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDate;
import com.github.silent.samurai.speedy.models.SpeedyDateTime;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyEnum;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.models.SpeedyTime;
import com.github.silent.samurai.speedy.models.SpeedyZonedDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isDateFormatValid;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isDateTimeFormatValid;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isTimeFormatValid;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isZonedDateTimeValid;

/// Decodes a raw string scalar into a typed {@link SpeedyValue} for the XML {@link StructureReader}.
/// XML has no native type information — every value arrives as a string — so this leaf decoder
/// parses text into the appropriate {@link SpeedyValue} subtype using the field's {@link ValueType}.
public final class SpeedyValueDecoder {

    private SpeedyValueDecoder() {
    }

    /// Decodes {@code raw} into a {@link SpeedyValue} of {@code field}'s type. Callers pass a
    /// non-empty scalar; an empty/absent value is a null token and is the caller's concern.
    public static SpeedyValue fromString(FieldMetadata field, String raw) throws SpeedyHttpException {
        return switch (field.getValueType()) {
            case ENUM -> new SpeedyEnum(raw, field);
            case ENUM_ORD -> {
                try {
                    yield new SpeedyEnum(Long.parseLong(raw), field);
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("expected number for ordinal enum field " + field.getOutputPropertyName());
                }
            }
            case DATE -> {
                if (!isDateFormatValid(raw)) {
                    throw new BadRequestException(String.format("Date value must be a string with ISO_DATE(%s) format",
                            LocalDate.now().format(DateTimeFormatter.ISO_DATE)));
                }
                yield new SpeedyDate(LocalDate.parse(raw, DateTimeFormatter.ISO_DATE));
            }
            case TIME -> {
                if (!isTimeFormatValid(raw)) {
                    throw new BadRequestException(String.format("Time value must be a string with ISO_TIME(%s) format",
                            LocalTime.now().format(DateTimeFormatter.ISO_TIME)));
                }
                yield new SpeedyTime(LocalTime.parse(raw, DateTimeFormatter.ISO_TIME));
            }
            case DATE_TIME -> {
                if (!isDateTimeFormatValid(raw)) {
                    throw new BadRequestException(String.format("DateTime value must be a string with ISO_DATE_TIME(%s) format",
                            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
                }
                yield new SpeedyDateTime(LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME));
            }
            case ZONED_DATE_TIME -> {
                if (!isZonedDateTimeValid(raw)) {
                    throw new BadRequestException(String.format("ZonedDateTime value must be a string with ISO_ZONED_DATE_TIME(%s) format",
                            ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                }
                yield new SpeedyZonedDateTime(ZonedDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            case BOOL -> {
                String lower = raw.toLowerCase();
                yield new SpeedyBoolean("true".equals(lower) || "1".equals(lower));
            }
            case TEXT -> new SpeedyText(raw);
            case INT -> {
                try {
                    yield new SpeedyInt(Long.parseLong(raw));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException(String.format(
                            "Not able to parse field %s with value type %s",
                            field.getOutputPropertyName(), field.getColumnType()));
                }
            }
            case FLOAT -> {
                try {
                    yield new SpeedyDouble(Double.parseDouble(raw));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException(String.format(
                            "Not able to parse field %s with value type %s",
                            field.getOutputPropertyName(), field.getColumnType()));
                }
            }
            case NULL -> SpeedyNull.SPEEDY_NULL;
            case OBJECT, COLLECTION -> throw new BadRequestException(String.format(
                    "Not able to parse field %s with value type %s",
                    field.getOutputPropertyName(), field.getColumnType()));
        };
    }
}
