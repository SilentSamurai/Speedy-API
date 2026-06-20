package com.github.silent.samurai.speedy.json.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.StructureReader;
import com.github.silent.samurai.speedy.models.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.silent.samurai.speedy.utils.CommonUtil.json;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isDateFormatValid;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isDateTimeFormatValid;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isTimeFormatValid;
import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.isZonedDateTimeValid;

/// JSON {@link StructureReader}: a thin StAX-style cursor over a Jackson
/// {@link JsonParser}. No document tree and no per-node objects — O(1) allocation
/// beyond the {@link SpeedyValue} result. Owns the leaf {@link ValueType} switch (the
/// read mirror of {@code writeLeaf}): it reads raw scalars straight off the parser and
/// constructs {@link SpeedyValue}s directly, with ENUM / ENUM_ORD built inline from
/// field metadata.
public class JsonStructureReader implements StructureReader {

    private final JsonParser parser;

    public JsonStructureReader(JsonParser parser) {
        this.parser = parser;
    }

    /// Opens a streaming JSON reader over the raw request body — the
    /// {@code byte[] -> StructureReader} factory the provider hands to the shared request
    /// parser (a {@link com.github.silent.samurai.speedy.interfaces.SpeedyRequestReader}). The
    /// read-side mirror of {@code JsonResponseWriter} being handed to the serializer.
    public static JsonStructureReader over(byte[] rawBody) throws SpeedyHttpException {
        try {
            return new JsonStructureReader(json().getFactory().createParser(rawBody));
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    /// Decodes a single value node (e.g., a query filter literal) by streaming it through
    /// the same leaf switch — keeping leaf decoding in one place.
    public static SpeedyValue decodeStandalone(JsonNode node, FieldMetadata field)
            throws SpeedyHttpException {
        try (JsonParser p = node.traverse()) {
            p.nextToken(); // position onto the value token
            return new JsonStructureReader(p).readField(field);
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public Kind begin() throws SpeedyHttpException {
        return kindOf(advance());
    }

    @Override
    public Kind currentKind() {
        return kindOf(parser.currentToken());
    }

    @Override
    public FieldMetadata nextField(EntityMetadata entityMetadata) throws SpeedyHttpException {
        String name;
        while ((name = nextKey()) != null) {
            if (entityMetadata.has(name)) {
                return entityMetadata.field(name);
            }
            skipValue(); // field unknown to the metadata — skip its value and continue
        }
        return null;
    }

    @Override
    public String nextKey() throws SpeedyHttpException {
        JsonToken t = advance();
        if (t == null || t == JsonToken.END_OBJECT) {
            return null;
        }
        if (t != JsonToken.FIELD_NAME) {
            throw new BadRequestException("Invalid JSON body");
        }
        String name;
        try {
            name = parser.currentName();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
        advance(); // move onto the field's value token
        return name;
    }

    @Override
    public Kind nextElement() throws SpeedyHttpException {
        JsonToken t = advance();
        if (t == null || t == JsonToken.END_ARRAY) {
            return null;
        }
        return kindOf(t);
    }

    @Override
    public SpeedyValue readField(FieldMetadata field) throws SpeedyHttpException {
        ValueType type = field.getValueType();
        JsonToken t = parser.currentToken();
        if (t == null || t == JsonToken.VALUE_NULL) {
            return SpeedyNull.SPEEDY_NULL;
        }
        try {
            return switch (type) {
                case ENUM -> {
                    if (t != JsonToken.VALUE_STRING) {
                        throw new BadRequestException("expected string for enum field " + field.getOutputPropertyName());
                    }
                    yield new SpeedyEnum(parser.getValueAsString(), field);
                }
                case ENUM_ORD -> {
                    if (!t.isNumeric()) {
                        throw new BadRequestException("expected number for ordinal enum field " + field.getOutputPropertyName());
                    }
                    yield new SpeedyEnum(parser.getValueAsLong(), field);
                }
                case DATE -> {
                    String s = parser.getValueAsString();
                    if (t != JsonToken.VALUE_STRING || !isDateFormatValid(s)) {
                        throw new BadRequestException(String.format("Date value must be a string with ISO_DATE(%s) format",
                                LocalDate.now().format(DateTimeFormatter.ISO_DATE)));
                    }
                    yield new SpeedyDate(LocalDate.parse(s, DateTimeFormatter.ISO_DATE));
                }
                case TIME -> {
                    String s = parser.getValueAsString();
                    if (t != JsonToken.VALUE_STRING || !isTimeFormatValid(s)) {
                        throw new BadRequestException(String.format("Time value must be a string with ISO_TIME(%s) format",
                                LocalTime.now().format(DateTimeFormatter.ISO_TIME)));
                    }
                    yield new SpeedyTime(LocalTime.parse(s, DateTimeFormatter.ISO_TIME));
                }
                case DATE_TIME -> {
                    String s = parser.getValueAsString();
                    if (t != JsonToken.VALUE_STRING || !isDateTimeFormatValid(s)) {
                        throw new BadRequestException(String.format("DateTime value must be a string with ISO_DATE_TIME(%s) format",
                                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
                    }
                    yield new SpeedyDateTime(LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME));
                }
                case ZONED_DATE_TIME -> {
                    String s = parser.getValueAsString();
                    if (t != JsonToken.VALUE_STRING || !isZonedDateTimeValid(s)) {
                        throw new BadRequestException(String.format("ZonedDateTime value must be a string with ISO_ZONED_DATE_TIME(%s) format",
                                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                    }
                    yield new SpeedyZonedDateTime(ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
                case BOOL -> new SpeedyBoolean(parser.getValueAsBoolean());
                case TEXT -> new SpeedyText(parser.getValueAsString());
                case INT -> new SpeedyInt(parser.getValueAsLong());
                case FLOAT -> new SpeedyDouble(parser.getValueAsDouble());
                case NULL -> SpeedyNull.SPEEDY_NULL;
                case OBJECT, COLLECTION -> throw new BadRequestException(String.format(
                        "Not able to parse field %s with value type %s",
                        field.getOutputPropertyName(), field.getColumnType()));
            };
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public String textValue() throws SpeedyHttpException {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return null;
        }
        try {
            return parser.getValueAsString();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public int intValue() throws SpeedyHttpException {
        try {
            return parser.getValueAsInt();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public boolean boolValue() throws SpeedyHttpException {
        try {
            return parser.getValueAsBoolean();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public boolean isBoolValue() {
        JsonToken t = parser.currentToken();
        return t == JsonToken.VALUE_TRUE || t == JsonToken.VALUE_FALSE;
    }

    @Override
    public void skipValue() throws SpeedyHttpException {
        JsonToken t = parser.currentToken();
        if (t == JsonToken.START_OBJECT || t == JsonToken.START_ARRAY) {
            try {
                parser.skipChildren();
            } catch (IOException e) {
                throw new BadRequestException("Invalid JSON body", e);
            }
        }
    }

    @Override
    public void close() {
        try {
            parser.close();
        } catch (IOException e) {
            // best-effort close of the streaming parser
        }
    }

    private JsonToken advance() throws SpeedyHttpException {
        try {
            return parser.nextToken();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    private static Kind kindOf(JsonToken t) {
        if (t == null) {
            return null;
        }
        return switch (t) {
            case START_OBJECT -> Kind.OBJECT;
            case START_ARRAY -> Kind.ARRAY;
            case VALUE_NULL -> Kind.NULL;
            default -> Kind.VALUE;
        };
    }
}
